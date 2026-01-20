package com.bit.idol.fanoutservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// feign 쓰면 보통 이거 붙이는게 안전
@SpringBootApplication
@EnableFeignClients
public class FanoutServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FanoutServiceApplication.class, args);
    }
}
