package com.bit.idol.rankingservice;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class RankingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RankingServiceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    // RedisTemplate 빈을 재정의하여 Serializer를 명확하게 설정
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) { // <String, String>으로 변경
        RedisTemplate<String, String> template = new RedisTemplate<>(); // <String, String>으로 변경
        template.setConnectionFactory(connectionFactory);

        // Key와 Value 모두 StringRedisSerializer 사용
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setStringSerializer(stringSerializer); // String 타입에 대한 Serializer도 명시

        // Hash 타입의 Key와 Value는 JSON으로 저장 (필요한 경우)
        // 현재 RankingService에서는 Hash를 String, String으로 사용하므로 주석 처리하거나 StringRedisSerializer 사용
        // template.setHashKeySerializer(new GenericJackson2JsonRedisSerializer()); 
        // template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
