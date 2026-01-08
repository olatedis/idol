package com.bit.docker.publicapi.service;

import com.bit.docker.publicapi.dto.ConcertResponse;
import com.bit.docker.publicapi.perse.KopisXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.List;

@Service
public class KopisService {

    @Value("${kopis.api.key}")
    private String apiKey;

    private static final String BASE_URL =
            "http://www.kopis.or.kr/openApi/restful/pblprfr";

    public List<ConcertResponse> getIdolConcerts(String startDate, String endDate) {

        String url = UriComponentsBuilder.fromPath(BASE_URL)
                .queryParam("service", apiKey)
                .queryParam("stdate", startDate)
                .queryParam("eddate", endDate)
                .queryParam("shcate", "CCCD") // 아이돌 콘서트
                .queryParam("rows", 50)
                .build()
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        String xml = restTemplate.getForObject(url, String.class);

        System.out.println(url);

        return KopisXmlParser.parse(xml);
    }
}

