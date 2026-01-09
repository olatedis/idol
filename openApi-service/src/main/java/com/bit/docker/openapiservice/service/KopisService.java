package com.bit.docker.openapiservice.service;

import com.bit.docker.openapiservice.dto.ConcertResponse;
import com.bit.docker.openapiservice.perse.KopisXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Service
public class KopisService {

    @Value("${kopis.api.key}")
    private String apiKey;

    private static final URI BASE_URL =
            URI.create("http://www.kopis.or.kr/openApi/restful/pblprfr");

    public List<ConcertResponse> getIdolConcerts(String startDate, String endDate, String pageNum) {

        String url = UriComponentsBuilder.fromUri(BASE_URL)
                .queryParam("service", apiKey)
                .queryParam("stdate", startDate)
                .queryParam("eddate", endDate)
                .queryParam("shcate", "CCCD")
                .queryParam("rows", 50)
                .queryParam("cpage", pageNum)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                byte[].class
        );

        String xml = new String(Objects.requireNonNull(response.getBody()), StandardCharsets.UTF_8);

        System.out.println(url);

        return KopisXmlParser.parse(xml);
    }
}

