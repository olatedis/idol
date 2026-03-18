package com.bit.subscriptionservice.client;

import com.bit.subscriptionservice.dto.IdolResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/idols/{idolId}")
    IdolResponse getIdol(@PathVariable("idolId") int idolId);
}
