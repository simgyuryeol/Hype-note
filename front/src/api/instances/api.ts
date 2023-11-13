import axios from "axios";
import Loading from "@/app/loading";
import useReissue from "@/hooks/useReissue";
// import { access } from "fs";
// import { validateHeaderValue } from "http";
// import { cookies } from "next/headers";

// axios 인스턴스 생성
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_SERVER_URL,
  headers: { "X-Custom-Header": "foobar" },
});

// 요청 인터셉터 설정
api.interceptors.request.use(
  function (config) {
    // 요청이 전달되기 전에 작업 수행

    // 토큰이 있는 경우
    const accessToken = localStorage.getItem("accessToken");
    console.log("11111111111111111111111111인터셉터옴!!!");
    if (accessToken) {
      config.headers["Authorization"] = `Bearer ${accessToken}`;
    }
    return config;
  },
  function (error) {
    // 요청 오류가 있는 작업 수행
    // 토큰이 없는 경우
    console.log(error);
    return Promise.reject(error);
  }
);

// 응답 인터셉터 추가하기
api.interceptors.response.use(
  function (response) {
    // 2xx 범위에 있는 상태 코드는 이 함수를 트리거 합니다.
    // 응답 데이터가 있는 작업 수행
    return response;
  },
  function (error) {
    // 2xx 외의 범위에 있는 상태 코드는 이 함수를 트리거 합니다.
    // 응답 오류가 있는 작업 수행

    // 에러가 토큰관련일 경우 함수 작성
    // 401 에러 토큰 만료
    // if (error.response.status === 401) {
    //   // redirect(`https://${window.location.origin}/api/auth/reissue`);
    //   window.location.href = process.env.NEXT_PUBLIC_SERVER_URL + "auth/reissue";
    // }
    console.log("22222222222222인터셉터옴!!!!!!!!!!!!");

    api.interceptors.response.use(
      function (response) {
        return response;
      },
      async function (error) {
        if (error.response.status === 401 || error.responese.status == 403) {
          try {
            const res = await axios.get(`${window.location.origin}/api/auth/reissue`, {
              withCredentials: true,
            });
            localStorage.setItem("accessToken", res.data.accessToken);

            // 이전 요청 다시 수행
            const originalRequest = error.config;
            originalRequest.headers["Authorization"] = `Bearer ${res.data.accessToken}`;
            return api(originalRequest);
          } catch (error) {
            return (window.location.href = `https://${window.location.origin}/signin`);
          }
        }
        return Promise.reject(error);
      }
    );

    // else if (error.response.status === 400) {
    //   redirect(`https://${window.location.origin}/api/auth/reissue`);
    // }

    return Promise.reject(error);
  }
);

export default api;
