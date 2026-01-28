package com.bit.docker.paymentservice.infra.toss;

import com.bit.docker.paymentservice.domain.dto.TossConfirmRequest;
import com.bit.docker.paymentservice.domain.dto.TossConfirmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPgClient {

    private final WebClient tossWebClient;

    @Value("${toss.secret-key}")
    private String secretKey;

    public TossConfirmResponse confirm(TossConfirmRequest request) {
        try {
            log.info("토스페이먼츠 API 호출: orderId={}, amount={}", request.getOrderId(), request.getAmount());
            
            TossConfirmResponse response = tossWebClient.post()
                    .uri("/v1/payments/confirm")
                    .headers(headers -> {
                        headers.setBasicAuth(secretKey, "");
                    })
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TossConfirmResponse.class)
                    .block();

            if (response == null) {
                log.error("토스페이먼츠 응답이 null: orderId={}", request.getOrderId());
                throw new RuntimeException("토스페이먼츠에서 빈 응답을 받았습니다.");
            }

            log.info("토스페이먼츠 API 응답: orderId={}, status={}", request.getOrderId(), response.getStatus());
            return response;
        } catch (WebClientResponseException e) {
            log.error("토스페이먼츠 API 오류 (상태: {}): orderId={}, message={}", 
                    e.getStatusCode(), request.getOrderId(), e.getResponseBodyAsString());
            throw new RuntimeException("토스페이먼츠 API 오류: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("토스페이먼츠 통신 오류: orderId={}, error={}", request.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("토스페이먼츠와의 통신 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}


