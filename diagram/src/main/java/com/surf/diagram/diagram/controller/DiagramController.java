package com.surf.diagram.diagram.controller;

import com.surf.diagram.diagram.common.BaseResponse;
import com.surf.diagram.diagram.dto.response.LinkResponseDto;
import com.surf.diagram.diagram.dto.response.NodeResponseDto;
import com.surf.diagram.diagram.dto.response.DiagramResponseDto;
import com.surf.diagram.diagram.entity.Link;
import com.surf.diagram.diagram.entity.Node;
import com.surf.diagram.diagram.repository.LinkRepository;
import com.surf.diagram.diagram.repository.NodeRepository;
import com.surf.diagram.diagram.service.DiagramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/diagram")
@Tag(name = "다이어그램", description = "다이어그램")
public class DiagramController {

    private final DiagramService diagramService;
    private final NodeRepository nodeRepository;
    private final LinkRepository linkRepository;

    public DiagramController(DiagramService diagramService, NodeRepository nodeRepository ,LinkRepository linkRepository) {
        this.diagramService = diagramService;
        this.nodeRepository = nodeRepository;
        this.linkRepository = linkRepository;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "내 노드와 링크 조회")
    public BaseResponse<DiagramResponseDto> getNodes(@PathVariable int userId) {
        DiagramResponseDto response = new DiagramResponseDto();
        List<Node> nodes = nodeRepository.findByUserId(userId);
        List<Link> links = linkRepository.findByUserId(userId);

        List<NodeResponseDto> nodeResponseDtos = new ArrayList<>();
        List<LinkResponseDto> linkResponseDtos = new ArrayList<>();

        for (Node node : nodes) {
            NodeResponseDto nodeResponseDto = new NodeResponseDto();
            nodeResponseDto.setId(node.getId());
            nodeResponseDto.setTitle(node.getTitle());
            nodeResponseDto.setUserId(node.getUserId());
            nodeResponseDto.setEditorId(node.getEditorId());
            nodeResponseDtos.add(nodeResponseDto);
        }

        for (Link link : links) {
            LinkResponseDto linkResponseDto = new LinkResponseDto();
            linkResponseDto.setSource(link.getSource());
            linkResponseDto.setTarget(link.getTarget());
            linkResponseDto.setUserId(link.getUserId());
            linkResponseDtos.add(linkResponseDto);
        }

        response.setNodes(nodeResponseDtos);
        response.setLinks(linkResponseDtos);
        return new BaseResponse<>(response);
    }


    @PostMapping("/node")
    @Operation(summary = "노드와 링크 생성")
    public void createNodeAndLink() {

        Node node = new Node();
        Link link = new Link();

        node.setUserId(1);
        node.setTitle("1");
        node.setEditorId(1);
        node.setContent("123");

        link.setTarget(1);
        link.setSource(2);
        link.setUserId(1);

        nodeRepository.save(node);
        linkRepository.save(link);
    }

    @PostMapping("/keyword/{userId}")
    @Operation(summary = "키워드 분석")
    public BaseResponse<String> classifyAndSaveEmptyCategoryDiagrams(@PathVariable int userId) throws Exception {
        diagramService.classifyAndSaveEmptyCategoryNodes(userId);
        return new BaseResponse<>("모든 Diagram들이 성공적으로 분석되고 업데이트되었습니다.");
    }

    @PostMapping("/link/{userId}")
    @Operation(summary = "링크 연결")
    public BaseResponse<String> linkNodesByCategoryAndConfidence(@PathVariable int userId) throws Exception {
        diagramService.linkNodesByCategoryAndConfidence(userId);
        return new BaseResponse<>("링크 생성 완료");
    }


    @PostMapping("/share/{userId}/{targetUserId}")
    @Operation(summary = "쉐어 노드")
    public BaseResponse<DiagramResponseDto> linkNodesByShare(@PathVariable int userId, @PathVariable int targetUserId) throws Exception {
        DiagramResponseDto responseDto = diagramService.linkNodesByShare(userId, targetUserId);
        return new BaseResponse<>(responseDto);
    }
}
