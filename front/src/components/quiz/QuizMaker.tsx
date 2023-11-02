"use client";

import React, { useMemo, useEffect, useState } from "react";
import { TreeSelect, Select, Button, message, Steps, theme } from "antd";
import type { SelectProps, RadioChangeEvent } from "antd";
import { useAtom } from "jotai";
import { isSoloAtom } from "../../store/isSolo";

const names: SelectProps["options"] = [
  { value: "가영", label: "가영" },
  { value: "자현", label: "자현" },
  { value: "규렬", label: "규렬" },
  { value: "세울", label: "세울" },
  { value: "상익", label: "상익" },
  { value: "인식", label: "인식" },
];

const handleChange = (value: string | string[]) => {
  console.log(`Selected: ${value}`);
};

export default function QuizMaker() {
  const { token } = theme.useToken();
  const [current, setCurrent] = useState(0);
  const [value, setValue] = useState(["0-0-0"]);
  const { SHOW_PARENT } = TreeSelect;
  const [isSolo] = useAtom(isSoloAtom);

  console.log("혼자냐?", isSolo);
  useEffect(() => {
    console.log("혼자냐?", isSolo);
  }, [isSolo]);

  const treeData = [
    {
      title: "MY CS BOOK",
      value: "0-0",
      key: "0-0",
      children: [
        {
          title: "네트워크",
          value: "0-0-0",
          key: "0-0-0",
        },
      ],
    },
    {
      title: "1주차",
      value: "0-1",
      key: "0-1",
      children: [
        {
          title: "네트워크",
          value: "0-1-0",
          key: "0-1-0",
        },
        {
          title: "운영체제",
          value: "0-1-1",
          key: "0-1-1",
        },
        {
          title: "자료구조",
          value: "0-1-2",
          key: "0-1-2",
        },
      ],
    },
  ];

  const tProps = {
    treeData,
    treeCheckable: true,
    showCheckedStrategy: SHOW_PARENT,
    placeholder: "페이지를 선택해주세요",
    style: {
      width: "300px",
      marginTop: "10px",
    },
  };

  const steps = useMemo(
    () => [
      {
        title: <h1 className="dark:text-font_primary">퀴즈 범위를 선택해주세요</h1>,
        content: (
          <div>
            <h1
              className="dark:text-font_primary text-dark_primary text-2xl"
              style={{ fontFamily: "preBd", marginTop: "30px", marginBottom: "10px", padding: 0 }}>
              퀴즈 범위
            </h1>
            <h1 className="dark:text-font_primary" style={{ marginBottom: "60px", padding: 0 }}>
              내 노트, 공유받은 페이지에서 퀴즈로 풀 페이지를 골라주세요.
            </h1>
            <TreeSelect {...tProps} />
          </div>
        ),
      },
      {
        title: <h1 className="dark:text-font_primary">문제 개수를 선택해주세요</h1>,
        content: (
          <div>
            <h1
              className="dark:text-font_primary text-dark_primary text-2xl"
              style={{ fontFamily: "preBd", marginTop: "30px", marginBottom: "10px", padding: 0 }}>
              문제 개수
            </h1>
            <h1 className="dark:text-font_primary" style={{ marginBottom: "60px", padding: 0 }}>
              페이지 당 생성 가능한 문제 수는 최소 1개, 최대 3개입니다.
            </h1>
            <Select
              defaultValue={{ value: "1", label: "1개" }}
              style={{ width: 120, marginTop: "10px" }}
              options={[
                { value: 1, label: "1개" },
                { value: 2, label: "2개" },
                { value: 3, label: "3개" },
              ]}
            />
          </div>
        ),
      },
      ...(isSolo
        ? []
        : [
            {
              title: <h1 className="dark:text-font_primary">같이 풀 친구를 초대하세요</h1>,
              content: (
                <div>
                  <h1
                    className="dark:text-font_primary text-dark_primary text-2xl"
                    style={{ fontFamily: "preBd", marginTop: "30px", marginBottom: "10px", padding: 0 }}>
                    친구 초대
                  </h1>
                  <h1 className="dark:text-font_primary" style={{ marginBottom: "60px", padding: 0 }}>
                    닉네임 검색을 통해 친구를 초대해보세요.
                  </h1>
                  <Select
                    defaultValue={["가영"]}
                    mode="tags"
                    size="middle"
                    placeholder="닉네임을 검색하세요"
                    onChange={handleChange}
                    style={{ width: "400px" }}
                    options={names}
                  />
                </div>
              ),
            },
          ]),
    ],
    []
  );

  const next = () => {
    setCurrent(current + 1);
  };

  const prev = () => {
    setCurrent(current - 1);
  };

  const items = steps.map((item) => ({ key: item.title, title: item.title }));

  const contentStyle: React.CSSProperties = {
    height: "350px",
    width: "900px",
    textAlign: "center",
    display: "flex",
    alignItems: "start",
    justifyContent: "center",
    color: token.colorTextTertiary,
    backgroundColor: token.colorFillAlter,
    // borderRadius: token.borderRadiusLG,
    border: `1px dashed ${token.colorBorder}`,
    marginTop: 16,
    fontFamily: "preRg",
  };

  return (
    <div className="items-center mx-auto">
      <h1 className="text-2xl font-bold text-center">퀴즈를 만들어봅시다</h1>
      <br />
      <Steps current={current} items={items} />
      <div style={contentStyle}>{steps[current].content}</div>
      <div style={{ display: "flex", justifyContent: "flex-end", fontFamily: "preRg", marginTop: 24 }}>
        {current > 0 && (
          <Button style={{ fontFamily: "preRg", backgroundColor: "white", margin: "0 8px" }} onClick={() => prev()}>
            이전
          </Button>
        )}
        {current < steps.length - 1 && (
          <Button
            className="dark:border dark:border-font_primary"
            style={{ fontFamily: "preRg", backgroundColor: "#2946A2" }}
            type="primary"
            onClick={() => next()}>
            다음
          </Button>
        )}
        {current === steps.length - 1 && (
          <Button
            className="dark:border dark:border-font_primary"
            style={{ fontFamily: "preRg", backgroundColor: "#2946A2" }}
            type="primary"
            onClick={() => message.success("생성 완료!")}>
            생성하기
          </Button>
        )}
      </div>
    </div>
  );
}
