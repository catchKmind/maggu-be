package com.maggu.maggu.auth.dto;

public record WithdrawResponse(
        boolean withdrawn
) {
    public static WithdrawResponse ok() {
        return new WithdrawResponse(true);
    }
}
