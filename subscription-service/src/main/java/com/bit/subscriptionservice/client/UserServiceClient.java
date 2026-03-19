package com.bit.subscriptionservice.client;

import com.bit.subscriptionservice.dto.GroupResponse;
import com.bit.subscriptionservice.dto.IdolResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/idols/{idolId}")
    IdolResponse getIdol(@PathVariable("idolId") int idolId);

    @GetMapping("/groups/bulk")
    List<GroupResponse> getGroupsByIds(@RequestParam("ids") List<Integer> ids);
}
