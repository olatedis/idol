package com.bit.subscriptionservice.client;

import com.bit.subscriptionservice.dto.BillingKeyResponse;
import com.bit.subscriptionservice.dto.TossBillingPaymentResponse;
import com.bit.subscriptionservice.entity.BillingKey;
import com.bit.subscriptionservice.repository.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Base64;

/**
 * Toss Payments 빌링키 관련 API 클라이언트
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TossBillingKeyClient {
    private static final String TOSS_API_BASE = "https://api.tosspayments.com/v1";

    private final RestTemplate restTemplate;
    private final BillingKeyRepository billingKeyRepository;

    @Value("${toss.secret-key}")
    private String secretKey;

    /**
     * authKey로 빌링키 발급
     * authKey: 결제 인증창에서 받은 일회성 인증 키
     * customerKey: 고객 식별자 (UUID 권장)
     */
    public BillingKeyResponse issueBillingKey(String authKey, String customerKey) {
        String url = TOSS_API_BASE + "/billing/authorizations/issue";

        String body = String.format("{\"authKey\":\"%s\",\"customerKey\":\"%s\"}", authKey, customerKey);

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            BillingKeyResponse response = restTemplate.postForObject(url, request, BillingKeyResponse.class);
            log.info("빌링키 발급 성공: customerKey={}, billingKey={}", customerKey, response.getBillingKey());
            return response;
        } catch (Exception e) {
            log.error("빌링키 발급 실패: {}", e.getMessage());
            throw new RuntimeException("빌링키 발급에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 빌링키를 이용한 정기결제 처리 (자동갱신 스케줄러에서 사용)
     * userId, idolId: 저장된 빌링키 조회용
     * amount: 결제 금액
     * orderId: 주문번호
     * orderName: 주문명
     */
    public TossBillingPaymentResponse processBillingPayment(
            int userId,
            int idolId,
            int amount,
            String orderId,
            String orderName
    ) {
        // 데이터베이스에서 활성 빌링키 조회
        BillingKey billingKeyEntity = billingKeyRepository
                .findByUserIdAndIdolIdAndActiveTrue(userId, idolId)
                .orElseThrow(() -> new RuntimeException("활성 빌링키를 찾을 수 없습니다: userId=" + userId + ", idolId=" + idolId));

        return processBillingPaymentInternal(
                billingKeyEntity.getBillingKey(),
                billingKeyEntity.getCustomerKey(),
                amount,
                orderId,
                orderName
        );
    }

    /**
     * 실제 정기결제 API 호출 (내부용)
     */
    private TossBillingPaymentResponse processBillingPaymentInternal(
            String billingKey,
            String customerKey,
            int amount,
            String orderId,
            String orderName
    ) {
        String url = TOSS_API_BASE + "/billing/" + billingKey;

        String body = String.format(
                "{\"customerKey\":\"%s\",\"amount\":%d,\"orderId\":\"%s\",\"orderName\":\"%s\",\"customerEmail\":\"noreply@idol.com\",\"customerName\":\"System\"}",
                customerKey, amount, orderId, orderName
        );

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            TossBillingPaymentResponse response = restTemplate.postForObject(url, request, TossBillingPaymentResponse.class);
            log.info("정기결제 처리 성공: billingKey={}, amount={}, orderId={}, paymentKey={}",
                    billingKey, amount, orderId, response != null ? response.getPaymentKey() : null);
            return response;
        } catch (Exception e) {
            log.error("정기결제 처리 실패 - billingKey: {}, amount: {}, orderId: {}, error: {}",
                    billingKey, amount, orderId, e.getMessage());
            throw new RuntimeException("정기결제 처리에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 빌링키 삭제
     */
    public void deleteBillingKey(String billingKey) {
        String url = TOSS_API_BASE + "/billing/" + billingKey;
        HttpHeaders headers = createHeaders();
        HttpEntity<String> request = new HttpEntity<>("", headers);

        try {
            restTemplate.delete(url, request);
            log.info("빌링키 삭제 성공: {}", billingKey);
        } catch (Exception e) {
            log.error("빌링키 삭제 실패: {}", e.getMessage());
            throw new RuntimeException("빌링키 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
        headers.set("Authorization", auth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}
