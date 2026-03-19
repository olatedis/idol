# 🌟 IDOL (Global Idol Interaction Platform)

> **"대규모 트래픽을 고려한 고가용성 마이크로서비스(MSA) 팬덤 플랫폼"**
>
> 전 세계 팬들과 아티스트가 실시간으로 소통하고 참여할 수 있는 고성능 실시간 채팅 및 투표 서비스입니다.
> CQRS 패턴과 이벤트 기반 아키텍처(Kafka)를 도입하여 쓰기 성능과 조회 성능을 모두 극대화하는 데 집중했습니다.

<br/>

## 🏗 System Architecture & Tech Stack

### 🛠 Tech Stack
- **Backend**: Java 21, Spring Boot 3.4
- **MSA Infra**: Spring Cloud (Gateway, Eureka, OpenFeign, Resilience4j)
- **Database / Cache**: MySQL, Spring Data JPA / MongoDB / Redis (PubSub, ZSet, Lua)
- **Messaging / Search**: Apache Kafka / Elasticsearch (Nori 분석기)
- **Frontend**: React 18, TypeScript, Zustand, STOMP.js

<br/>

## 🚀 Core Technical Challenges & Deep Dive

이 프로젝트는 수만 명의 팬이 동시다발적으로 상호작용할 때 발생하는 **데이터 정합성 보장**과 **트래픽 병목 해결**에 포커스를 맞추었습니다.

---

### 1. 💬 Chat Service: 실시간 채팅 시스템 (트래픽 폭주 대비 및 비동기 파이프라인)

수만 명의 팬과 아이돌이 상주하는 채팅방의 특성상 트래픽 폭주와 로딩 병목 현상을 해결하기 위해 설계했습니다.

- **동작 흐름 및 아키텍처**
  1. **WebSocket 연결 및 인증**: 클라이언트가 HandShake를 요청하면 Spring `ChannelInterceptor`가 JWT 토큰을 검증해 세션에 유저 정보를 저장합니다.
  2. **메시지 전송 및 전처리**: Redis를 활용해 초당 전송 횟수를 제한(도배 방지)하고, 비속어를 필터링합니다.
  3. **저장 및 이벤트 발행 (Outbox Pattern)**: 메시지는 MongoDB에 `PENDING` 상태로 저장되고, Kafka Producer가 `chat-message-topic`으로 메시지를 수신합니다. (`idolId`를 Partition Key로 사용하여 메시지 순서 완벽 보장)
  4. **실시간 브로드캐스팅**: Kafka Consumer가 메시지를 읽고 **Redis Pub/Sub** 채널로 발행하여 다중 서버 환경의 구독자들에게 안정적으로 전파합니다.
  5. **AI 비동기 텍스트 검열**: "자살/폭탄" 등의 의심 단어가 감지되면 비동기로 별도 토픽에 이벤트를 발행, 외부 AI API 검수를 진행하여 채팅 전송 성능에 영향을 주지 않도록 설계했습니다.

- **Troubleshooting & Q&A**
  - **Q. 채팅방에 사람이 몰리면 서버가 터지지 않나요?**
    A. Kafka를 버퍼로 사용하여 트래픽 폭주를 흡수하고, Redis Pub/Sub을 통해 다중 서버(Scale-out) 환경에서도 메시지를 안정적으로 브로드캐스트합니다.
  - **Q. 채팅 내역이 너무 많아지면 조회가 느려지지 않나요?**
    A. 가장 많이 조회되는 최신 메시지 50건은 **Redis List**에 캐싱(Latency 1ms 이내)하고, 누적 데이터는 **MongoDB** 샤딩으로 처리하며, 키워드 검색은 **Elasticsearch**로 위임했습니다.
  - **Q. AI 검열 호출 시 지연 시간 발생과 비용 문제는?**
    A. 1차 정규식/Aho-Corasick 필터링 후 의심 메시지만 '비동기'로 통신망을 타도록 설계하여 비용과 속도를 동시에 확보했습니다.

---

### 2. 🗳 Vote Service (투표 시스템): 트래픽 폭주 대비 및 동시성 완벽 제어

연말 시상식 등 특정 이벤트 기간에 폭발적으로 몰리는(Spiky Traffic) 트래픽의 병목과 데드락(Deadlock) 한계를 극복했습니다.

- **Redis Lua Script 원자적 연산 (Atomic Operation)**: 투표 중복 참여 방지를 위해 DB Lock을 피하고, "키 확인 -> 시간 설정 -> 완료 마킹" 과정을 완전한 원자적 스크립트로 처리하여 Race Condition을 완벽 차단.
- **서킷 브레이커(Circuit Breaker) & 지연 처리 폴백(Fallback)**: 트래픽이 `RateLimiter` 한계치를 초과하거나 Redis 장애 시, 에러를 뱉지 않고 **Kafka로 투표 요청을 비동기 큐잉(Queueing)**하여 DB에 안전하고 유연하게 적재.
- **어뷰징 방지 기반 차단**: Redis Set 구조와 만료(TTL)를 결합, 단일 IP의 비정상적 시도(1분 내 10회 이상) 즉시 차단 및 블랙리스트 로직.

---

### 3. 🏆 Ranking Service: ZSet을 활용한 O(log(N)) 실시간 랭킹 산출

초당 수만 건의 득표 현황을 RDBMS의 `ORDER BY`로 매번 계산하는 구조적 불가능을 회피했습니다.

- **Redis Sorted Set (ZSET)**: 수백만 표가 몰려도 `opsForZSet().incrementScore`를 사용하여 O(log(N)) 속도로 실시간 정렬 유지.
- **실시간 Delta 계산 및 WebSocket 푸시**: 주기적으로 랭킹을 브로드캐스팅할 때, **Redis Hash**를 이용해 이전 점수와 현재 점수를 비교, 등락폭(Delta)을 프론트에 실시간으로 푸시.

---

### 4. 🔗 데이터 최종 일관성 (CQRS & Event-Driven Architecture)

Write 부하와 Read 병목 지점을 해결하기 위해 명령/조회를 엄격히 분리하고, 비동기 파이프라인으로 일관성을 유지합니다.

- **시나리오 (검색 시스템 동기화)**: 게시판 작성(MySQL Write) 시, 즉시 `board-post-index-topic` Kafka 이벤트를 발행. `search-service`의 Consumer가 이를 받아 **Elasticsearch**에 Bulk Index(UPSERT) 처리. 무거운 `%LIKE%` 쿼리 없이 ES가 조회(Read)를 전담.
- **지연(Delay) 극복 전략**: CQRS의 고질적인 'Eventual Consistency' 특성상 발생하는 밀리초 단위 지연감을 상쇄하기 위해, 본인 작성 글은 프론트 단 낙관적 업데이트(Optimistic UI) 또는 1차 DB 조회 방식으로 풀고, 검색 포털 뷰만 지연을 허용하는 형태의 트레이드오프 설계 채택.

---

### 5. ⚡ MSA 통신 네트워크 한계 돌파 (Feign vs gRPC 튜닝 시나리오)

채팅 등 극도로 빠른 실시간 응답이 필수인 환경에서, 내부 도메인(`chat-service` -> `user-service` 인가 검증) 간 동기식 API 호출 지연을 타파하기 위한 최적화 과정입니다.

- **문제점 파악 및 성능 비교 (100회 실행, 가상 유저 50명 k6 Load Test)**
  - `OpenFeign (REST/JSON)`: 평균 571.6ms / TPS 73.8
  - `gRPC (Binary)`: 평균 272.3ms / TPS 133.4 **(약 2.1배 고속)**
- **의사 결정 방향 (Trade-off)**: 성능 상으론 압도적인 gRPC 였으나, .proto 작성이라는 러닝 커브와 이진(Binary) 데이터 디버깅이라는 '협업 리스크'를 고려, **기존 OpenFeign의 속도를 한계까지 최적화(Tuning)하는 방안** 선택.
- **튜닝 내역 및 성과**
  - **Keep-Alive (연결 재사용)**: `Apache HttpClient 5` 도입으로 3-Way Handshake 비용을 제거 (Stateful 전환).
  - **GZIP Data Compression**: JSON 공백/중복 문자 압축으로 네트워크 페이로드 대역폭 경량화.
  - **결과**: `(전) 571ms` -> `(후) 놀라운 Latency 개선`, 기존 통신 속도의 **최대 5배 끌어올리며**, 유지보수성을 포기하지 않고 실시간 성능 최적화를 성공적으로 달성.

<br/>

## ⚙️ Quick Start

**Prerequisites**: Docker 및 Docker Compose가 필요합니다.

1. 저장소를 클론합니다.
   ```bash
   git clone https://github.com/your-repo/idol.git
   cd idol
   ```
2. 전체 인프라(DB, Kafka 등) 및 서비스를 백그라운드에서 실행합니다.
   ```bash
   docker-compose up -d
   ```
3. 서비스 접속:
   - **Frontend**: `http://localhost:5173`
   - **API Gateway**: `http://localhost:8000`
   - **Eureka Dashboard**: `http://localhost:8761`

---
*© 2026 IDOL Project. Designed for High Availability and Scalable Fandom Configurations.*
