package com.rbihub.loan.dto.request;

import com.rbihub.loan.domain.enums.LoanPurpose;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanDto(
        @NotNull(message = "loan.amount is required")
        @DecimalMin(value = "10000", message = "loan.amount must be between 10,000 and 50,00,000")
        @DecimalMax(value = "5000000", message = "loan.amount must be between 10,000 and 50,00,000")
        BigDecimal amount,

        @Min(value = 6, message = "loan.tenureMonths must be between 6 and 360")
        @Max(value = 360, message = "loan.tenureMonths must be between 6 and 360")
        int tenureMonths,

        @NotNull(message = "loan.purpose is required")
        LoanPurpose purpose
) {
}
