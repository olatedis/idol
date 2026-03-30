# IDOL — Global Idol Interaction Platform

> **"대규모 트래픽을 고려한 고가용성 마이크로서비스(MSA) 팬덤 플랫폼"**
>
> 전 세계 팬들과 아티스트가 실시간으로 소통하고 참여할 수 있는 고성능 실시간 채팅 및 투표 서비스입니다.
> CQRS 패턴과 이벤트 기반 아키텍처(Kafka)를 도입하여 쓰기 성능과 조회 성능을 모두 극대화하는 데 집중했습니다.

<br/>

## 기술 스택

| 분류 | 기술 |
|---|---|
| **언어 / 프레임워크** | Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0 |
| **MSA 인프라** | Spring Cloud Gateway (Reactive), Netflix Eureka, OpenFeign HC5, Resilience4j, Bucket4j, ShedLock |
| **데이터베이스** | MySQL 8 (JPA), MongoDB (채팅), Elasticsearch 8 (Nori 형태소 분석기) |
| **캐시 / 메시지** | Redis 7 (Pub/Sub, ZSET, Lua, 분산 락, Rate Limiter), Apache Kafka |
| **통신** | STOMP WebSocket, SSE, gRPC (Protobuf 3.25), REST (OpenFeign) |
| **프론트엔드** | React 18, TypeScript, Zustand, STOMP.js |
| **DevOps** | Docker, Docker Compose, GitHub Actions CI/CD (변경 감지 선택적 배포) |

<br/>

## 아키텍처

```
Client
  │
  ▼
API Gateway (Reactive, JWT 필터, Redis Rate Limiter)
  │
  ├─ eureka-server       (서비스 디스커버리)
  ├─ auth-service        (JWT 발급, Kakao OAuth2, gRPC 인터페이스)
  ├─ user-service        (유저 프로필, 아이돌/그룹 관리)
  ├─ chat-service        (STOMP WebSocket, AI 모더레이션, DeepL 번역)
  ├─ board-service       (게시판, 댓글)
  ├─ vote-service        (투표, Outbox 패턴, Redis Lua 멱등성)
  ├─ ranking-service     (Redis ZSET 실시간 랭킹 + WebSocket 브로드캐스트)
  ├─ reserve-service     (좌석 예매, Redis 분산 락)
  ├─ payment-service     (Toss Payments PG 연동)
  ├─ subscription-service (구독 라이프사이클, 자동 갱신)
  ├─ fanout-service      (알림 팬아웃 워커)
  ├─ notify-service      (SSE 알림 딜리버리, 멀티탭 지원)
  └─ search-service      (Elasticsearch 한국어 전문 검색)
```

**데이터 저장소 분리 전략**
| 저장소 | 용도 | 선택 이유 |
|---|---|---|
| MySQL | 트랜잭션 데이터 (투표, 예매, 결제, 유저) | ACID, 정합성 우선 |
| MongoDB | 채팅 메시지 (`idolId + _id` 복합 인덱스) | 수평 확장, 스키마 유연성 |
| Redis | 세션, 분산 락, 캐시, Pub/Sub, ZSET 랭킹 | 인메모리 저지연 |
| Elasticsearch | 채팅/게시글 한국어 전문 검색 | 역색인, Nori 형태소 분석 |

<br/>

## 핵심 기술 구현

---

### 1. 실시간 채팅 — STOMP + Kafka + Redis Pub/Sub

수만 명이 동시 접속하는 채팅방의 트래픽 폭주와 멀티 서버 브로드캐스팅 문제를 해결했습니다.

**메시지 처리 흐름**
```
STOMP 메시지 전송
  → Redis Rate Limit (userId 기준 1초 5회)
  → 비속어 필터 (Aho-Corasick)
  → MongoDB PENDING 저장
  → Kafka 발행 (idolId를 Key → 동일 파티션 → 순서 보장)
  → Consumer → Redis Pub/Sub → 전체 WebSocket 서버 브로드캐스팅
  → 의심 단어 감지 시 별도 토픽으로 비동기 AI 검열
```

**CQRS 기반 채팅 조회**
- 최근 50개: Redis List 캐싱 (3일 TTL) → 1ms 이내 응답
- 키워드 검색: Elasticsearch 위임
- 원본 저장: MongoDB (`idolId + _id` 복합 인덱스로 커서 페이지네이션)
- 번역 결과: `ChatMessage.translations: Map<String, String>` 내장 저장 → 동일 메시지 재번역 없음

**Troubleshooting**

> **Q. 채팅방 사람이 몰리면 서버가 터지지 않나요?**
> Kafka를 버퍼로 사용해 트래픽 폭주를 흡수하고, Redis Pub/Sub으로 다중 서버에 안정적으로 브로드캐스트합니다.

> **Q. AI 검열로 채팅 전송이 느려지지 않나요?**
> 1차 Aho-Corasick + Leet-speak 정규화(`0→o`, `1→i`, `@→a`)로 의심 메시지만 필터링 후, 별도 Kafka 토픽으로 비동기 OpenAI Moderation API 호출합니다. 채팅 전송 속도에 영향 없음.

---

### 2. 동시성 제어 — 좌석 예매 & 투표

**좌석 예매 — Redis 분산 락 + TTL 만료 방어**
- `SETNX`로 락 획득, UUID 값으로 소유자 식별
- DB 트랜잭션 완료 후 `verifyLock`으로 UUID 재확인 → TTL이 느린 처리 중 만료되어 다른 요청이 락 획득하는 엣지케이스 방어
- 책임 분리: `ReservationService`(락) → `ReservationHandler`(DB 트랜잭션) → `ReservationEventListener`(`AFTER_COMMIT` Kafka 발행)

**투표 중복 방지 — Redis Lua 원자 스크립트**
- `EXISTS → SET → EXPIRE` 3개 명령을 Lua 스크립트로 단일 라운드트립에 원자 처리 → Race Condition 완전 차단
- Redis 장애 시 Resilience4j `@CircuitBreaker` → DB 체크 + Kafka 직접 발행으로 폴백
- 폴백 자체도 `@RateLimiter(name = "vote-db-protection")`으로 DB 과부하 방지

---

### 3. 데이터 일관성 — Outbox 패턴 + AFTER_COMMIT

DB-Kafka 이중 쓰기 문제(DB 저장 후 서버 크래시 → 이벤트 유실)를 두 가지 패턴으로 해결했습니다.

**Transactional Outbox Pattern (vote-service)**
```
[castVote 트랜잭션]
  → vote 데이터 저장
  → outbox_events 테이블에 이벤트 기록  ← 동일 트랜잭션
  → 커밋

[OutboxScheduler — 5초 주기]
  → processed = false 레코드 조회
  → Kafka 발행
  → processed = true 업데이트
  → 발행 실패 시 미처리 유지 → 다음 폴링에서 재시도
```

**`@TransactionalEventListener(AFTER_COMMIT)`**
- DB 커밋 성공 후에만 Kafka 이벤트 발행 → 롤백된 트랜잭션의 이벤트 발행 방지

---

### 4. 보안 — 다층 방어

**JWT + Refresh Token Rotation**
- Refresh Token은 Redis에 저장 (TTL 7일)
- 재발급 시 Redis 저장 토큰과 불일치 → 탈취로 판단, 해당 userId의 전체 토큰 삭제 → 공격자/피해자 모두 재로그인 강제
- 로그아웃 시 만료 토큰에서도 `getClaims()`로 userId 추출해 Redis 정리

**API Gateway Redis 유저 상태 확인**
- 모든 요청에서 JWT 검증 후 `user:info:id::{userId}` Redis 조회
- SUSPENDED/BANNED 유저는 DB 쿼리 없이 즉시 차단
- Redis 장애 시 Graceful Fallback으로 트래픽 미차단
- 다운스트림으로 `X-User-Id`, `X-Role`, `X-Username` 헤더 전파

**이중 Rate Limiting**
- API Gateway: Spring Cloud Gateway `RequestRateLimiter` (Redis 토큰 버킷)
  - `/auth/**`: IP 기준 30 req/s, `/votes/**`: userId 기준 50 req/s
- auth-service: Bucket4j 인메모리 5 req/min (서킷브레이커와 함께 DB 보호)

**브루트포스 방어**
- `login:fail:{username}` Redis 카운터 → 5회 실패 시 30분 락 + Kafka `LOGIN_FAIL_LOCKED` 알림 이벤트

---

### 5. 실시간 랭킹 — Redis ZSET + ShedLock

초당 수만 건의 득표를 RDBMS `ORDER BY`로 매번 계산하는 방식을 회피했습니다.

- `ZSET.incrementScore`로 O(log N) 실시간 정렬 유지
- `@Scheduled(fixedRate = 1000)`: 매 1초 ZSET 조회 → Redis Hash의 이전 점수와 Delta 비교 → WebSocket 브로드캐스트
- 멀티 파드 중복 실행 방지: `@SchedulerLock(lockAtLeastFor = "PT0.5S", lockAtMostFor = "PT0.9S")`
- 1위 변경 시 `RANKING_CHANGED` Kafka 알림 이벤트 자동 발행

---

### 6. MSA 통신 최적화 — Feign vs gRPC

채팅 인증 경로(`chat-service` → `auth-service` 토큰 검증)의 동기 호출 지연을 타파하기 위한 최적화 과정입니다.

**k6 부하 테스트 결과 (100회, 가상 유저 50명)**
| | OpenFeign (REST/JSON) | gRPC (Protobuf) |
|---|---|---|
| 평균 응답 | 571.6ms | 272.3ms |
| TPS | 73.8 | 133.4 |

**의사 결정**: gRPC가 약 2.1배 빠르지만 `.proto` 작성 러닝커브와 바이너리 디버깅 협업 리스크를 고려, **OpenFeign 튜닝** 방향 선택

**튜닝 내역**
- Apache HttpClient 5 도입 → Keep-Alive 연결 재사용 (매 요청 3-Way Handshake 제거)
- GZIP 압축으로 JSON 페이로드 경량화
- 결과: 571ms → **최대 5배 성능 향상**, 유지보수성 포기 없이 실시간 성능 달성

---

### 7. 성능 최적화

**결제 트랜잭션 분리 (`PaymentService.confirm`)**
- ① `validatePayment`: 읽기 전용 트랜잭션
- ② `tossPgClient.confirm(...)`: **트랜잭션 없음** — 외부 HTTP 호출 중 DB 커넥션 미점유
- ③ `completePayment`/`markAsFailed`: 짧은 쓰기 트랜잭션
- 느린 PG API 응답(2~3초)으로 인한 커넥션 풀 고갈 방지

**Kafka 배치 컨슈머 + JdbcTemplate 벌크 업데이트**
- `List<String>` 배치 수신 후 인메모리 후보 캐시로 N+1 Feign 쿼리 방지
- 개별 JPA `save` → `JdbcTemplate.batchUpdate`로 투표 수 일괄 반영

---

### 8. DevOps — 변경 감지 선택적 CI/CD

14개 서비스 전체를 매번 빌드하면 CI 시간이 너무 길어집니다.

- `tj-actions/changed-files@v44`로 변경된 서비스 디렉토리 감지
- 변경된 서비스만 빌드 → Docker Hub push → EC2 SSH 배포
- `nohup ... &` 백그라운드 실행으로 GitHub Actions SSH timeout 배포 중단 방지
- 멀티스테이지 Dockerfile: `amazoncorretto:21-jdk AS builder` → JRE runtime만 복사
- `GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"` — CI OOM 방지

<br/>


