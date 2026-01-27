package com.bit.idol.chatservice.controller;

import com.bit.idol.chatservice.client.AuthFeignClient;
import com.bit.idol.chatservice.client.AuthGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/benchmark")
@RequiredArgsConstructor
@Slf4j
public class BenchmarkController {

    private final AuthFeignClient authFeignClient;
    private final AuthGrpcClient authGrpcClient;

    @GetMapping("/feign")
    public ResponseEntity<Map<String, Object>> testFeign(@RequestHeader("Authorization") String token) {
        long start = System.nanoTime();
        Map<String, Object> result = authFeignClient.verifyToken(token);
        long end = System.nanoTime();
        
        long duration = (end - start) / 1000; // microseconds
        log.info("Feign Call Duration: {} us", duration);
        
        result.put("duration_us", duration);
        result.put("type", "FEIGN");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/grpc")
    public ResponseEntity<Map<String, Object>> testGrpc(@RequestHeader("Authorization") String token) {
        // Bearer 제거
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        long start = System.nanoTime();
        Map<String, Object> result = authGrpcClient.verifyToken(token);
        long end = System.nanoTime();
        
        long duration = (end - start) / 1000; // microseconds
        log.info("gRPC Call Duration: {} us", duration);
        
        result.put("duration_us", duration);
        result.put("type", "GRPC");
        return ResponseEntity.ok(result);
    }
}
