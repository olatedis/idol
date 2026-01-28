package com.bit.docker.subscriptionservice.entity;

public enum SubscriptionPlan {
    MONTHLY(9900, "월간"),
    ANNUAL(89100, "연간");  // 월간 * 12 * 0.9 = 89,100원

    private final int amount;
    private final String displayName;

    SubscriptionPlan(int amount, String displayName) {
        this.amount = amount;
        this.displayName = displayName;
    }

    public int getAmount() {
        return amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDurationInMonths() {
        return this == MONTHLY ? 1 : 12;
    }

    public double getDiscount() {
        return this == MONTHLY ? 0 : 0.1;  // 10% 할인
    }
}
