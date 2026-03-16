# 🌟 IDOL (Global Idol Interaction Platform)

> **"팬과 아티스트를 하나로 잇는 고가용성 마이크로서비스 플랫폼"**
>
> IDOL 프로젝트는 대규모 트래픽 환경에서의 안정적인 소통과 공정한 참여를 지원하기 위해 설계된 MSA 기반 글로벌 팬덤 플랫폼입니다.

---

## 🏗 Architecture Overview

본 프로젝트는 **Microservices Architecture(MSA)**를 기반으로 서비스 간 독립적인 확장성과 장애 격리를 보장합니다.

- **Communication**: STOMP + WebSocket 기반 실시간 채팅 및 Kafka 비동기 이벤트 전파.
- **Data Strategy**: CQRS 패턴 및 Polyglot Persistence (MySQL, Redis, MongoDB, Elasticsearch).
- **Resilience**: API Gateway 라우팅 및 Resilience4j 기반의 Circuit Breaker 적용.

---

## 🚀 Key Implementation Highlights

### ⚡ Performance & Scalability
- **Hybrid Caching Strategy**: Redis(Hot Data)와 MongoDB(Warm/Cold Data)를 결합하여 채팅 처리량 극대화.
- **Atomic Operations**: Redis Lua Script를 활용한 투표 로직의 원자성 보장 및 경합 해결.
- **Network Optimization**: Gzip 압축 및 커넥션 풀링(HttpClient 5) 설정을 통한 서비스 간 지연 시간 단축.

### 🛡 Reliability & Security
- **Transactional Outbox Pattern**: 데이터 저장과 이벤트 메시지 발행의 원자성을 확보하여 서비스 간 최종 정합성 준수.
- **Aho-Corasick & AI Filtering**: 고성능 패턴 매칭 알고리즘과 비동기 AI 분석을 통한 비용 효율적 채팅 정화 시스템.
- **Deep Link Notification**: UX 최적화를 위한 인앱 페이지 직결 딥링크 기술 적용.

### 🔍 Search & Data
- **Search Engine Enrichment**: Elasticsearch (Nori 분석기) 기반의 정밀한 공연 및 아티스트 검색 서비스.
- **QueryDSL Integration**: 컴파일 타임 타입 체크를 통한 안정적인 동적 쿼리 엔진 구축.

---

## 🛠 Tech Stack

### Backend
- **Core**: Java 21, Spring Boot 3.4
- **MS Infrastructure**: Spring Cloud (Eureka, Gateway, OpenFeign, Config)
- **Persistence**: Spring Data JPA, Hibernate, QueryDSL, Spring Data Redis/MongoDB/Elasticsearch
- **Messaging**: Apache Kafka

### Frontend
- **Framework**: React, TypeScript, Vite
- **Interaction**: Framer Motion, STOMP.js
- **State Management**: Zustand / Recoil

### Infrastructure/DevOps
- **Container**: Docker & Docker Compose
- **Monitoring**: Zipkin, Micrometer (Tracing)

---

## ⚙️ Quick Start

**전제 조건**: Docker 및 Docker Compose가 설치되어 있어야 합니다.

1. 저장소를 클론합니다.
   ```bash
   git clone https://github.com/your-repo/idol.git
   cd idol
   ```

2. 인프라 및 전체 서비스를 기동합니다.
   ```bash
   docker-compose up -d
   ```

3. 서비스 접속:
   - Frontend: `http://localhost:5173`
   - API Gateway: `http://localhost:8000`
   - Eureka Dashboard: `http://localhost:8761`

---

## 📅 Development Journey
- **개발 기간**: 2026.01.05 - 2026.03.20
- **단계**: 요구사항 분석 → MSA 인프라 구축 → 핵심 도메인 고도화 → 통합 성능 최적화

---
© 2026 IDOL Project Team. All rights reserved.
