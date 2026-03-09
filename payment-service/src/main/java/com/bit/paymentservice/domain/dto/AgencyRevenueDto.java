package com.bit.paymentservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyRevenueDto {
    private int agencyId;
    private long totalRevenue;
    private long concertRevenue;
    private long subscriptionRevenue;
}
