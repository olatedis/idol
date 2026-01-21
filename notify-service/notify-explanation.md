발신 서비스들은 "대상규칙(targetType/targetId)"만 보내고
fanout-service가 "유저 목록 확장/분배" 담당
notify-service는 "저장/조회/실시간 전달" 담당

eventId(UUID) : 이벤트 전역 고유값(중복방지)
type : 알림 종류
targetType : 누구에게? (USER/ALL/IDOL_SUB/GROUP_SUB...)
targetId : 구체적 대상(예: userId, idolId, groupId...)
args : 템플릿 변수(Map)
redirectUrl : 클릭 시 이동 경로
occurredAt : 발생 시각

======= . ========

1. 다른 서비스에서 알림을 보내고 싶다. notify-request-topic로 보냄 (Kafka)

2. fanout-service가 consumer로 notify-request-topic을 받아
   targetType = USER, targetId = 각 userId 로 바꿈

3. notify-service가 notify-fanout-topic을 소비
   클라이언트는 경로따라 조회하거나 sse로 실시간 확인