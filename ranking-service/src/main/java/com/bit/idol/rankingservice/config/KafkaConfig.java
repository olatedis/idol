package com.bit.idol.rankingservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    /**
     * Kafka Consumer 에러 핸들러 설정
     * - 메시지 처리 실패 시 재시도(Retry) 후, 최종 실패 시 DLQ(Dead Letter Queue)로 이동
     */
    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        
        // 1. 죽은 편지(DLQ) 발송자 설정
        // 실패한 메시지를 '원래토픽이름.DLT' (예: vote-complete-topic.DLT)로 보냄
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> {
                    log.error("메시지 처리 최종 실패. DLQ로 이동합니다. Topic: {}, Offset: {}, Error: {}", 
                            record.topic(), record.offset(), ex.getMessage());
                    return null; // 기본 설정(topic.DLT) 사용
                });

        // 2. 에러 핸들러 설정 (1초 간격으로 3번 재시도)
        FixedBackOff backOff = new FixedBackOff(1000L, 3);
        
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
