import { createQuizRoom, inviteUser } from "@/api/service/quiz";
import { QuizRoomGroup, QuizUser } from "@/types/quiz";
import { useState } from "react";
import { useMutation } from "react-query";

export const useCreateRoom = () => {
  const [roomInfo, setRoomInfo] = useState<QuizRoomGroup>();

  const createRoomMutation = useMutation(
    async ({
      roomName,
      pages,
      sharePages,
      quizCnt,
      single,
    }: {
      roomName: string;
      pages: Array<number>;
      sharePages: Array<number>;
      quizCnt: number;
      single: boolean;
    }) => {
      const response = await createQuizRoom(roomName, pages, sharePages, quizCnt, single);
      console.log(response.data.result);
      setRoomInfo(response.data.result);
    },
    {
      onError: (error) => {
        console.log(error);
      },
    }
  );
  const inviteUserMutation = useMutation(
    async ({
      roomName,
      pages,
      sharePages,
      quizCnt,
      single,
      users,
    }: {
      roomName: string;
      pages: Array<number>;
      sharePages: Array<number>;
      quizCnt: number;
      single: boolean;
      users: Array<QuizUser>;
    }) => {
      const response = await inviteUser(roomName, pages, sharePages, quizCnt, single, users);
      console.log(response.data.result);
    },
    {
      onError: (error) => {
        console.log(error);
      },
    }
  );

  return {
    createRoomMutation: createRoomMutation,
    inviteUserInfo: roomInfo ? roomInfo.inviteUsers : undefined,
    roomInfo: roomInfo,
    inviteUserMutation: inviteUserMutation,
  };
};
