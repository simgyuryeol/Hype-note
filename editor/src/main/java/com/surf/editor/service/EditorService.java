package com.surf.editor.service;

import com.surf.editor.common.error.ErrorCode;
import com.surf.editor.common.error.exception.BaseException;
import com.surf.editor.common.error.exception.NotFoundException;
import com.surf.editor.domain.Editor;
import com.surf.editor.dto.request.*;
import com.surf.editor.dto.response.*;
import com.surf.editor.feign.client.MemberOpenFeign;
import com.surf.editor.feign.client.MemberShareOpenFeign;
import com.surf.editor.feign.client.MemberUnShareOpenFeign;
import com.surf.editor.feign.dto.MemberEditorSaveRequestDto;
import com.surf.editor.feign.dto.MemberShareRequestDto;
import com.surf.editor.repository.EditorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class EditorService {
    private final EditorRepository editorRepository;
    private final MemberOpenFeign memberOpenFeign;
    private final MemberShareOpenFeign memberShareOpenFeign;
    private final MemberUnShareOpenFeign memberUnShareOpenFeign;


    @Transactional
    public EditorCreateResponseDto editorCreate(int userId) {
        Editor savedEditor = new Editor();
        try{
            /**
             * 새로 만들면 부모가 null, 자식도 새로 만들어진다.
             */
            savedEditor = editorRepository.save(Editor.editorCreate(userId));
        }catch (Exception e){
            throw new BaseException(ErrorCode.FAIL_CREATE_EDITOR);
        }

        //feign member, quiz, diagram DB에도 저장
        memberCreateFeign(userId, savedEditor);

        EditorCreateResponseDto editorCreateResponseDto = EditorCreateResponseDto.builder()
                .id(savedEditor.getId())
                .build();

        return editorCreateResponseDto;
    }

    private void memberCreateFeign(int userId, Editor savedEditor) {
        try{
            MemberEditorSaveRequestDto memberEditorSaveRequestDto = MemberEditorSaveRequestDto.builder()
                    .userId(userId)
                    .root(savedEditor.getId()) //추가 필요
                    .build();

            memberOpenFeign.MemberEditorSave(memberEditorSaveRequestDto);
        }catch (Exception e){
            throw new BaseException(ErrorCode.MEMBER_SAVE_FAIL);
        }
    }

    public void editorWrite(String editorId, EditorWriteRequestDto editorWriteRequest) {
        Editor byId = editorRepository.findById(editorId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        try{
            byId.write(editorWriteRequest.getTitle(),editorWriteRequest.getContent());
            editorRepository.save(byId);
        }catch (Exception e){
            throw new BaseException(ErrorCode.FAIL_WRITE_EDITOR);
        }

    }

    public void editorDelete(String editorId) {
        Editor byId = editorRepository.findById(editorId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        //부모의 나 제거
        Editor parentEditor = editorRepository.findById(byId.getParentId()).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));
        parentEditor.childDelete(byId.getId());
        editorRepository.save(parentEditor);

        //자녀 제거
        for (String childId : byId.getChildId()) {
            Editor childEditor = editorRepository.findById(childId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));
            childEditor.parentDelete();
            editorRepository.save(childEditor);
        }

        try{
            editorRepository.delete(byId);
        }catch (Exception e){
            throw new BaseException(ErrorCode.FAIL_DELETE_EDITOR);
        }
    }

    public EditorCheckResponseDto editorCheck(String editorId) {
        Editor byId = editorRepository.findById(editorId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        EditorCheckResponseDto editorCheckResponse = EditorCheckResponseDto.builder()
                .id(byId.getId())
                .title(byId.getTitle())
                .content(byId.getContent())
                .build();

        return editorCheckResponse;
    }

    public EditorSearchResponseDto editorSearch(String search) {
        List<Editor> byTitleContainingOrContentContaining =
                editorRepository.findByTitleContainingOrContentContaining(search, search)
                        .orElse(null);

        List<EditorSearchResponseDto.Editors> editors = new ArrayList<>();

        for (Editor editor : byTitleContainingOrContentContaining) {

            editors.add(
                    EditorSearchResponseDto.Editors.builder()
                    .id(editor.getId())
                    .title(editor.getTitle())
                    .content(editor.getContent())
                    .build());
        }
        EditorSearchResponseDto editorList = EditorSearchResponseDto.builder()
                .notes(editors)
                .build();

        return editorList;
    }


    public void editorRelation(int userId, EditorRelationRequestDto editorRelationRequestDto) {
        // db의 child 리스트에 추가
        Editor findParentEditor = editorRepository.findById(editorRelationRequestDto.getParentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        Editor findChildEditor = editorRepository.findById(editorRelationRequestDto.getChildId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        findParentEditor.childRelation(editorRelationRequestDto.getChildId());

        // root가 아니라면 해당 기존의 parent에서 child값을 삭제해야 한다.
        if(!findChildEditor.getParentId().equals("root")){
            Editor findPreParentEditor = editorRepository.findById(findChildEditor.getParentId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

            findPreParentEditor.childDelete(findChildEditor.getId());
            editorRepository.save(findPreParentEditor);
        }

        findChildEditor.parentRelation(editorRelationRequestDto.getParentId());

        editorRepository.save(findParentEditor);
        editorRepository.save(findChildEditor);
    }

    // 하이퍼 링크 부모는 바뀌지 않는다.
    public void editorHyperLink(int userId, EditorHyperLinkRequestDto editorHyperLinkRequestDto) {
        // db의 hyperLink 리스트에 추가
        Editor findEditor = editorRepository.findById(editorHyperLinkRequestDto.getParentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        findEditor.childHyperLink(editorHyperLinkRequestDto.getChildId());

        editorRepository.save(findEditor);
    }

    /**
     *  에디터 쓰기 권한
     *  에디터 별로 쓰기 리스트 생성한 후 쓰기 설정하면 유저 id를 넣어주고 아니면 빼준다.
     *  유저가 해당 에디터에 접속할 때 에디터 상세정보에서 쓰기 권한 true,false를 리턴해준다.
     */
    public void editorWriterPermission(EditorWriterPermissionRequestDto editorWriterPermissionRequestDto) {
        Editor editor = editorRepository.findById(editorWriterPermissionRequestDto.getEditorId()).orElseThrow(
                ()-> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        try{
            if(editorWriterPermissionRequestDto.isWritePermission()){
                editor.writerPermissionAdd(editorWriterPermissionRequestDto.getSharedId());
            }else{
                if(editor.getWritePermission().contains(editorWriterPermissionRequestDto.getEditorId())){
                    editor.writerPermissionSub(editorWriterPermissionRequestDto.getSharedId());
                }
            }
        }catch (Exception e){
            throw new BaseException(ErrorCode.WRITER_PERMISSION_FAIL);
        }

    }

    public List<EditorListResponseDto> editorList(EditorListRequestDto editorListRequestDto) {

        List<String> rootList = editorListRequestDto.getRootList();

        List<EditorListResponseDto> editorListResponseDtoList = new ArrayList<>();
        for (String root : rootList) {
            Editor editor = editorRepository.findById(root).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));
            EditorListResponseDto treeDto = convertEditorToTreeDto(editor);
            editorListResponseDtoList.add(treeDto);
        }

        return editorListResponseDtoList;
    }

    private EditorListResponseDto convertEditorToTreeDto(Editor editor) {
        EditorListResponseDto editorListResponseDto = new EditorListResponseDto();

        editorListResponseDto.setId(editor.getId());
        editorListResponseDto.setTitle(editor.getTitle());
        editorListResponseDto.setParentId(editor.getParentId());
        editorListResponseDto.setOwner(editor.getUserId());

        List<String> childIds = editor.getChildId();
        if(childIds !=null && !childIds.isEmpty()){
            List<EditorListResponseDto> children = new ArrayList<>();

            for (String childId : childIds) {
                Editor childEditor = editorRepository.findById(childId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));
                EditorListResponseDto childDto = convertEditorToTreeDto(childEditor);
                children.add(childDto);
            }
            editorListResponseDto.setChildren(children);
        }
        return editorListResponseDto;
    }


    /**
     * 문서 ID List에 공유 되어 잇는 인원 교집합 리턴
     * 1. 문서 리스트 순회
     * 2. 해당 문서에서 첫번째 값에 유저를 딕셔너리에 넣는다.
     * 3. 이후 차례대로 해당 딕셔너리 값이 있으면 값을 +1 더하고 없으면 해당 딕셔너리를 삭제한다.
     * 4. 마지막으로 남은 딕셔너리 값이 문서 갯수와 같으면 교집합이므로 리턴해준다.
     */
    public EditorShareMemberResponseDto editorShareMember(EditorShareMemberRequestDto editorShareMemberRequestDto) {
        List<Integer> userList = new ArrayList<>();
        Map<Integer,Integer> editorDict = new HashMap<>();

        List<String> editorList = editorShareMemberRequestDto.getEditorList();
        for (String editorId : editorList) {
            Editor editor = editorRepository.findById(editorId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

            for (Integer sharedUser : editor.getSharedUser()) {
                if(editorDict.containsKey(sharedUser)){ //값이 있으면 +1
                    editorDict.put(sharedUser,editorDict.get(sharedUser)+1);
                }
                else{
                    editorDict.put(sharedUser,1);
                }
            }
        }

        for (Integer key : editorDict.keySet()) {
            if(editorDict.get(key)== editorList.size()){
                userList.add(key);
            }
        }

        EditorShareMemberResponseDto editorShareMemberResponseDto = EditorShareMemberResponseDto.builder()
                .userList(userList)
                .build();

        return editorShareMemberResponseDto;
    }

    /**
     * 1. 해당 문서에 공유하는 인원 추가
     * 2. member에게 feign 보냄
     */
    public void editorShare(EditorShareRequestDto editorShareRequestDto) {
        // 1. 해당 문서에 공유하는 인원 추가
        String editorId = editorShareRequestDto.getEditorId();
        Editor editor = editorRepository.findById(editorId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        editorShareChild(editor,editorShareRequestDto.getUserList(),0);

        // 2. member에게 feign 보냄
        editorShareFeign(editorShareRequestDto.getEditorId(),editorShareRequestDto.getUserList(),0);

    }

    public void editorUnshare(EditorUnShareRequestDto editorUnShareRequestDto) {
        // 1. 공유 인원 해제
        Editor editor = editorRepository.findById(editorUnShareRequestDto.getEditorId()).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));

        editorShareChild(editor,editorUnShareRequestDto.getUserList(),1);

        // 2. member에게 feign 보냄
        editorShareFeign(editorUnShareRequestDto.getEditorId(), editorUnShareRequestDto.getUserList(),1);
    }

    private void editorShareFeign(String editorId, List<Integer> userList, int type) {
        try{
            MemberShareRequestDto memberShareRequestDto = MemberShareRequestDto.builder()
                    .editorId(editorId)
                    .userPkList(userList)
                    .build();

            if(type==0){
                memberShareOpenFeign.MemberShare(memberShareRequestDto);
            }
            else{
                memberUnShareOpenFeign.MemberUnShare(memberShareRequestDto);
            }

        }catch (Exception e){
            throw new BaseException(ErrorCode.MEMBER_SAVE_FAIL);
        }
    }


    private void editorShareChild(Editor editor, List<Integer> userList,int type) {
        for (int userId : userList) {
            if(!editor.getSharedUser().contains(userId)){
                if(type==0){ //0이면 추가
                    editor.sharedUserAdd(userId);
                }
                else{ //1이면 삭제
                    editor.sharedUserSub(userId);
                }

            }
        }
        editorRepository.save(editor);

        List<String> childIds = editor.getChildId();
        if(childIds != null && !childIds.isEmpty()){

            for (String childId : childIds) {
                Editor childEditor = editorRepository.findById(childId).orElseThrow(() -> new NotFoundException(ErrorCode.EDITOR_NOT_FOUND));
                editorShareChild(childEditor,userList,type);
            }
        }
    }
}