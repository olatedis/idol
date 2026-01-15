package com.bit.idol.fanoutservice.client;

import com.bit.idol.fanoutservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "user-service", url = "${clients.user-service.url}")
public interface UserServiceClient {
    // client/는 fanout이 외부 서비스에 요청해서 데이터 가져오는 통로

    @GetMapping("/internal/users/info/all")
    List<UserDto> getAllUsers();

    // 지금은 일단 전체명단호출만 일시적으로 만듬. 추가적으로 더 만들면 됨

}
