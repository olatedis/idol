package com.bit.reserveservice.infra.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Repository
@Slf4j
public class SeatLockRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public SeatLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean lock(int concertId, int seatId, int userId) {
        String key = "seat:lock:%d:%d".formatted(concertId, seatId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(userId), Duration.ofMinutes(2));

        log.info("좌석 락 시도: key={}, userId={}, success={}", key, userId, success);
        return Boolean.TRUE.equals(success);
    }

    public void unlock(int concertId, int seatId) {
        String key = "seat:lock:%d:%d".formatted(concertId, seatId);
        Boolean deleted = redisTemplate.delete(key);
        log.info("좌석 락 해제: key={}, deleted={}", key, deleted);
    }

    // 락 소유자 확인
    public boolean verifyLock(int concertId, int seatId, int userId) {
        String key = "seat:lock:%d:%d".formatted(concertId, seatId);
        String ownerId = redisTemplate.opsForValue().get(key);
        boolean isOwner = ownerId != null && ownerId.equals(String.valueOf(userId));
        log.debug("락 소유자 확인: key={}, userId={}, ownerId={}, isOwner={}", key, userId, ownerId, isOwner);
        return isOwner;
    }
}
