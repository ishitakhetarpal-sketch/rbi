package com.rbihub.loan.config;

import com.rbihub.loan.domain.enums.EmploymentType;
import com.rbihub.loan.domain.enums.RiskBand;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Externalized lending configuration. Every business threshold and premium
 * is declared here so business rules can be tuned without code changes.
 */
@Validated
@ConfigurationProperties(prefix = "lending")
public record LendingProperties(
        @NotNull BigDecimal baseInterestRate,
        @NotNull Map<RiskBand, BigDecimal> riskPremiums,
        @NotNull Map<EmploymentType, BigDecimal> employmentPremiums,
        @NotNull LoanSize loanSize,
        @NotNull Eligibility eligibility,
        @NotNull RiskBands riskBands
) {

    public record LoanSize(@NotNull BigDecimal threshold, @NotNull BigDecimal premiumAboveThreshold) {
    }

    public record Eligibility(
            int minCreditScore,
            int maxAgePlusTenureYears,
            @NotNull BigDecimal maxEmiToIncomeRatio,
            @NotNull BigDecimal maxOfferEmiToIncomeRatio
    ) {
    }

    public record RiskBands(int lowMinScore, int mediumMinScore, int highMinScore) {
    }
}
