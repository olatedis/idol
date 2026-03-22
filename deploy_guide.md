# AWS 운영 서버 배포 준비 가이드

모든 마이크로서비스의 운영 설정(`application-prod.yml`) 및 도커 설정(`docker-compose-prod.yml`)이 완료되었습니다.

## 1. 📂 생성된 파일 목록
- [x] `docker-compose-prod.yml`: 전체 서비스 운영 오케스트레이션
- [x] 각 서비스별 `src/main/resources/application-prod.yml`:
    - `discovery-service`, `api-gateway`, `auth-service`, `user-service`, `chat-service`, `board-service`, `vote-service`, `search-service`, `notification-service`, `ranking-service`

## 2. 🔑 필수 환경 변수 설정 (.env)
EC2 서버의 프로젝트 루트 폴더에 `.env` 파일을 생성하고 다음 내용을 입력하세요.

```properties
AWS_PUBLIC_IP=13.124.XXX.XXX  # EC2 탄력적 IP
DB_PASSWORD=your_secure_password # MySQL용 루트 비밀번호
```

## 3. 🚀 서버 실행 명령어
```bash
# 로컬 설정을 기본으로 하되 운영 전용 파일로 덮어쓰기 하여 실행
docker-compose -f docker-compose.yml -f docker-compose-prod.yml up -d --build
```

## 4. ⚠️ 주의 사항
- `user-service`: DB 스키마 자동 생성을 위해 첫 실행 시 `hibernate.ddl-auto: update`로 설정되어 있습니다.
- `search-service`: 초기 검색 인덱스 데이터가 없을 수 있으므로, 게시글 작성 시 Kafka 이벤트를 통해 색인이 진행됩니다.
- `chat-service`: MongoDB와 Kafka가 정상 동작해야 채팅 가능합니다.
