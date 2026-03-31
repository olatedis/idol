package com.bit.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdolResponse {
    private int idolId;
    private int agencyId;
    private Integer groupId;
    private String groupName;
}
