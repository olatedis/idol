package com.bit.docker.subscriptionservice.service;

import com.bit.docker.subscriptionservice.client.TossBillingKeyClient;
import com.bit.docker.subscriptionservice.dto.BillingKeyRequest;
import com.bit.docker.subscriptionservice.dto.BillingKeyResponse;
import com.bit.docker.subscriptionservice.entity.BillingKey;
import com.bit.docker.subscriptionservice.repository.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * 빌링키 관리 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BillingKeyService {
    private final BillingKeyRepository billingKeyRepository;
    private final TossBillingKeyClient tossBillingKeyClient;

    /**
     * 빌링키 발급 및 저장
     * 1. Toss API에서 빌링키 발급
     * 2. 데이터베이스에 저장
     * 3. 다음 자동갱신부터 이 빌링키 사용
     */
    public BillingKey issueBillingKey(
            String authKey,
            int userId,
            int idolId,
            String customerKey
    ) {
        log.info("빌링키 발급 시작: userId={}, idolId={}", userId, idolId);

        // Toss API에서 빌링키 발급
        BillingKeyResponse tossResponse = tossBillingKeyClient.issueBillingKey(authKey, customerKey);

        // 기존 빌링키 비활성화 (같은 고객의 같은 아이돌 구독)
        billingKeyRepository.findByUserIdAndIdolIdAndActiveTrue(userId, idolId)
                .ifPresent(BillingKey::deactivate);

        // 새 빌링키 저장
        BillingKey billingKey = BillingKey.builder()
                .customerKey(customerKey)
                .userId(userId)
                .idolId(idolId)
                .billingKey(tossResponse.getBillingKey())
                .cardNumber(tossResponse.getCard().getNumber())
                .cardIssuer(tossResponse.getCard().getIssuerCode())
                .cardType(tossResponse.getCard().getCardType())
                .active(true)
                .build();

        BillingKey savedKey = billingKeyRepository.save(billingKey);
        log.info("빌링키 저장 완료: billingKey={}", savedKey.getId());

        return savedKey;
    }

    /**
     * 저장된 빌링키 조회
     */
    public BillingKey getBillingKey(int userId, int idolId) {
        return billingKeyRepository.findByUserIdAndIdolIdAndActiveTrue(userId, idolId)
                .orElseThrow(() -> new RuntimeException("빌링키를 찾을 수 없습니다"));
    }

    /**
     * 빌링키 삭제 (구독 취소 시)
     */
    public void deleteBillingKey(int userId, int idolId) {
        BillingKey billingKey = getBillingKey(userId, idolId);
        
        // Toss API에서 빌링키 삭제
        tossBillingKeyClient.deleteBillingKey(billingKey.getBillingKey());
        
        // 데이터베이스에서 비활성화
        billingKey.deactivate();
        billingKeyRepository.save(billingKey);
        
        log.info("빌링키 삭제 완료: userId={}, idolId={}", userId, idolId);
    }

    /**
     * 빌링키 존재 여부 확인
     */
    public boolean hasBillingKey(int userId, int idolId) {
        return billingKeyRepository.findByUserIdAndIdolIdAndActiveTrue(userId, idolId).isPresent();
    }
}
