package com.rbihub.loan.service.impl;

import com.rbihub.loan.config.LendingProperties;
import com.rbihub.loan.domain.enums.EmploymentType;
import com.rbihub.loan.domain.enums.RiskBand;
import com.rbihub.loan.service.InterestRateCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class TieredInterestRateCalculator implements InterestRateCalculator {

    private final LendingProperties properties;

    public TieredInterestRateCalculator(LendingProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal calculate(RiskBand riskBand, EmploymentType employmentType, BigDecimal loanAmount) {
        Objects.requireNonNull(riskBand, "riskBand must not be null");
        Objects.requireNonNull(employmentType, "employmentType must not be null");
        Objects.requireNonNull(loanAmount, "loanAmount must not be null");

        BigDecimal rate = properties.baseInterestRate()
                .add(premiumFor(properties.riskPremiums(), riskBand))
                .add(premiumFor(properties.employmentPremiums(), employmentType))
                .add(loanSizePremium(loanAmount));
        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal loanSizePremium(BigDecimal loanAmount) {
        LendingProperties.LoanSize cfg = properties.loanSize();
        return loanAmount.compareTo(cfg.threshold()) > 0 ? cfg.premiumAboveThreshold() : BigDecimal.ZERO;
    }

    private static <K> BigDecimal premiumFor(java.util.Map<K, BigDecimal> table, K key) {
        BigDecimal premium = table.get(key);
        if (premium == null) {
            throw new IllegalStateException("missing premium configuration for " + key);
        }
        return premium;
    }
}
