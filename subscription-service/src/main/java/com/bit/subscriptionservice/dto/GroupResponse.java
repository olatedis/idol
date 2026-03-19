package com.bit.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {
    private int groupId;
    private String name;
    private String groupImage;
}
