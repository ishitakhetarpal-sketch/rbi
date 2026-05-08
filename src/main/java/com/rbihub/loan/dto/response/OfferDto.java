package com.rbihub.loan.dto.response;

import java.math.BigDecimal;

public record OfferDto(
        BigDecimal interestRate,
        int tenureMonths,
        BigDecimal emi,
        BigDecimal totalPayable
) {
}
