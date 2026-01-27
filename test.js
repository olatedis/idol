import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 설정 (동시 접속자 50명, 30초 동안 부하 발생)
export let options = {
  vus: 50,
  duration: '30s',
};

export default function () {
  let token = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxMDAwIiwidXNlcm5hbWUiOiJ0ZXN0dXNlciIsIm5pY2tuYW1lIjoi7YWM7Iqk7Yq47Jyg7KCAIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3Njk0OTI2MzMsImV4cCI6MTc2OTQ5MzgzM30.5DV4SDYioob5upJwCvP2AMHy7yGPjESy94RI9RIsaryEZ51Ic1LS8AwQt6uh2t7A";
  
  let params = {
    headers: {
      'Authorization': 'Bearer ' + token,
    },
  };

  // === 테스트할 URL 선택 (주석 해제/처리) ===
  
  // 1. Feign (REST) 테스트
  //let res = http.get('http://host.docker.internal:8089/benchmark/feign', params);
  
  // 2. gRPC 테스트
   let res = http.get('http://host.docker.internal:8089/benchmark/grpc', params);

  check(res, { 'status was 200': (r) => r.status == 200 });
  sleep(0.1); // 0.1초 대기
}
