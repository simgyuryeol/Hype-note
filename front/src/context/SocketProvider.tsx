"use client";

import SockJS from "sockjs-client";
import { useEffect, useState, createContext } from "react";
import { Stomp, CompatClient } from "@stomp/stompjs";
import React, { ReactNode } from "react";
import { QuizRoomInfo } from "@/types/quiz";

interface Props {
  children: ReactNode;
}

export const SocketContext = createContext<{
  client: CompatClient | null;
  quizRooms: Array<QuizRoomInfo>;
  room: QuizRoomInfo | null;
  setRoomNumber: (roomNumber: number | null) => void;
  sendReady: (roomNumber: number) => void;
  sendUnReady: (roomNumber: number) => void;
  sendOutRoom: (roomNumber: number) => void;
  sendRooms: () => void;
  sendMessage: (roomNumber: number, messageInput: object) => void;
}>({
  client: null,
  quizRooms: [],
  room: null,
  setRoomNumber: () => {},
  sendReady: () => {},
  sendUnReady: () => {},
  sendOutRoom: () => {},
  sendRooms: () => {},
  sendMessage: () => {},
});

// 소켓 기본 연결
export const SocketProvider: React.FC<Props> = ({ children }) => {
  const [roomNumber, setRoomNumber] = useState<number | null>(null);
  const [quizRooms, setQuizRooms] = useState([]);
  const [room, setRoom] = useState<QuizRoomInfo | null>(null);

  const socketFactory = () => new SockJS(process.env.NEXT_PUBLIC_SERVER_URL + "quiz/stomp/ws");
  const client = Stomp.over(socketFactory);

  // const socket = new SockJS(process.env.NEXT_PUBLIC_SERVER_URL + "quiz/stomp/ws");
  // const client = Stomp.over(socket);

  // 룸리스트 연결
  useEffect(() => {
    client.connect({}, () => {
      console.log("서버와 연결!", client.connected);

      // 지금 현재 방목록 불러오기
      if (quizRooms.length == 0) {
        subscribeRoomList();
      }
      // 룸 연결
      console.log(roomNumber);
      if (roomNumber) {
        console.log(roomNumber, "null아님");
        sendInRoom(roomNumber);
        subscribeChat(roomNumber);
      }
    });

    // return () => {
    // client.disconnect(() => {
    // console.log("서버 연결 해제");
    // });
    // };
  }, [roomNumber, client]);

  const subscribeRoomList = () => {
    client.subscribe("/sub/quizroom/roomList", (roomList) => {
      setQuizRooms(JSON.parse(roomList.body));
      console.log(roomList.body);
    });
    sendRooms();
  };

  const sendRooms = () => {
    // if (quizRooms.length === 0) {
    //   console.log("방리스트구독!", client.connected);
    //   subscribeRoomList();
    // }

    client.send("/pub/quizroom/roomList", {});
  };

  // 방 구독
  const subscribeInRoom = (roomId: number) => {
    client.subscribe(`/sub/quiz/${roomId}`, (response) => {
      const responseBody = JSON.parse(response.body);
      setRoom(responseBody.result);
    });
  };

  // 입장
  const sendInRoom = (roomId: number) => {
    subscribeInRoom(roomId);

    const data = {
      userPk: "2",
      userName: "isc",
    };

    client.send(`/pub/quizroom/in/${roomId}`, {}, JSON.stringify(data));
  };

  //퇴장
  const sendOutRoom = (roomId: number) => {
    const data = {
      userPk: "2",
    };

    client.send(`/pub/quizroom/out/${roomId}`, {}, JSON.stringify(data));

    client.unsubscribe(`/sub/quiz/${roomId}`);

    setRoomNumber(null);
  };

  // 레디
  const sendReady = (roomId: number) => {
    console.log("레디합니데이");
    const data = {
      userPk: 2,
      action: "ready",
    };

    client.send(`/pub/quizroom/ready/${roomId}`, {}, JSON.stringify(data));
  };

  // 언레디
  const sendUnReady = (roomId: number) => {
    console.log("언레디");
    const data = {
      userPk: 2,
      action: "unready",
    };
    client.send(`/pub/quizroom/ready/${roomId}`, {}, JSON.stringify(data));
  };

  //채팅방 구독
  const subscribeChat = (roomId: number) => {
    client.subscribe(`/sub/chat/${roomId}`, (message) => {
      console.log("채팅", message);
    });
  };
  // 채팅 전송
  const sendMessage = (roomId: number, messageInput: object) => {
    client.send(`/pub/chat/${roomId}`, {}, JSON.stringify(messageInput));
  };

  return (
    <SocketContext.Provider
      value={{ client, quizRooms, setRoomNumber, room, sendReady, sendOutRoom, sendRooms, sendUnReady, sendMessage }}>
      {children}
    </SocketContext.Provider>
  );
};
