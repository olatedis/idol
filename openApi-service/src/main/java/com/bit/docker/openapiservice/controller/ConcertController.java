package com.bit.docker.openapiservice.controller;

import com.bit.docker.openapiservice.dto.ConcertResponse;
import com.bit.docker.openapiservice.service.KopisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ConcertController {

    private final KopisService kopisService;

    public ConcertController(KopisService kopisService) {
        this.kopisService = kopisService;
    }

    @GetMapping("/api/concerts/idol")
    public List<ConcertResponse> getIdolConcerts(@RequestParam String startDate, @RequestParam String endDate, @RequestParam String pageNum) {
        return kopisService.getIdolConcerts(startDate, endDate, pageNum);
    }
}

