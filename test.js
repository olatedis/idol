import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 설정 (동시 접속자 50명, 30초 동안 부하 발생)
export let options = {
  vus: 50,
  duration: '30s',
};

export default function () {
  let token = "yJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJ1c2VyMSIsIm5pY2tuYW1lIjoi7KeA66-87YysIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3NzAxNjU4NDUsImV4cCI6MTc3MDE2NzA0NX0.3b57_jtj5n4JR_xM1u0MoXpA738K3QTnhda1VStcGqGB5cNNpQSyQTWozRmRW5AZ";
  
  let params = {
    headers: {
      'Authorization': 'Bearer ' + token,
    },
  };

  // === 테스트할 URL 선택 (주석 해제/처리) ===
  
  // 1. Feign (REST) 테스트
  let res = http.get('http://host.docker.internal:8089/benchmark/feign', params);
  
  // 2. gRPC 테스트
  // let res = http.get('http://host.docker.internal:8089/benchmark/grpc', params);

  // 에러 발생 시 로그 출력 (디버깅용)
  if (res.status !== 200) {
      console.log(`Error: status=${res.status}, body=${res.body}`);
  }

  check(res, { 'status was 200': (r) => r.status == 200 });
  sleep(0.1); // 0.1초 대기
}
