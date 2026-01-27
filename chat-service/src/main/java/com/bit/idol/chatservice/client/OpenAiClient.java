package com.bit.idol.chatservice.client;

import com.bit.idol.chatservice.dto.openai.ModerationRequest;
import com.bit.idol.chatservice.dto.openai.ModerationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "openai-client", url = "https://api.openai.com/v1")
public interface OpenAiClient {

    @PostMapping("/moderations")
    ModerationResponse checkModeration(
            @RequestHeader("Authorization") String apiKey,
            @RequestBody ModerationRequest request
    );
}
