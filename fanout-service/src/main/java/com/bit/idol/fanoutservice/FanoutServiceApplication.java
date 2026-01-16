package com.bit.idol.fanoutservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class FanoutServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FanoutServiceApplication.class, args);
    }

}
