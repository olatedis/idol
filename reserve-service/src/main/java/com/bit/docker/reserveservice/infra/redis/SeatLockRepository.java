package com.bit.docker.reserveservice.infra.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class SeatLockRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public SeatLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean lock(Long concertId, Long seatId, Long userId) {
        String key = "seat:lock:%d:%d".formatted(concertId, seatId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId.toString(), Duration.ofMinutes(3));

        return Boolean.TRUE.equals(success);
    }

    public void unlock(Long concertId, Long seatId) {
        redisTemplate.delete("seat:lock:%d:%d".formatted(concertId, seatId));
    }
}
