package com.bit.reserveservice.infra.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class SeatLockRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public SeatLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean lock(int concertId, int seatId, int userId) {
        String key = "seat:lock:%d:%d".formatted(concertId, seatId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(userId), Duration.ofMinutes(3));

        return Boolean.TRUE.equals(success);
    }

    public void unlock(int concertId, int seatId) {
        redisTemplate.delete("seat:lock:%d:%d".formatted(concertId, seatId));
    }

    // 락 소유자 확인
    public boolean verifyLock(int concertId, int seatId, int userId) {
        String key = "seat:lock:%d:%d".formatted(concertId, seatId);
        String ownerId = redisTemplate.opsForValue().get(key);
        return ownerId != null && ownerId.equals(String.valueOf(userId));
    }
}
