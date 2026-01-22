package com.bit.docker.boardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

// idol-service 내부 검증api
// TODO: 아이돌서비스 엔드포인트에 맞추기

@FeignClient(name = "idol-service", url = "${clients.idol-service.url}")
public interface IdolInternalClient {

    // (IDOL role) userId가 해당 idolId 본인인지 확인
    @GetMapping("/internal/idols/{idolId}/is-owner/{userId}")
    boolean isOwner(@PathVariable("idolId") Long idolId,
                    @PathVariable("userId") Integer userId,
                    @RequestHeader("X-Internal-Key") String internalKey);
}
