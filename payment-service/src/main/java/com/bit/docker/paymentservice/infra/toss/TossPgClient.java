package com.bit.docker.paymentservice.infra.toss;

import com.bit.docker.paymentservice.domain.dto.TossConfirmRequest;
import com.bit.docker.paymentservice.domain.dto.TossConfirmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
@Component
@RequiredArgsConstructor
public class TossPgClient {

    private final WebClient tossWebClient;

    @Value("${toss.secret-key}")
    private String secretKey;

    public TossConfirmResponse confirm(TossConfirmRequest request) {
        return tossWebClient.post()
                .uri("/v1/payments/confirm")
                .headers(headers -> {
                    headers.setBasicAuth(secretKey, "");
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }
}


