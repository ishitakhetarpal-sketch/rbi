package com.rbihub.loan.service;

import com.rbihub.loan.TestLendingProperties;
import com.rbihub.loan.domain.enums.EmploymentType;
import com.rbihub.loan.domain.enums.RiskBand;
import com.rbihub.loan.service.impl.TieredInterestRateCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TieredInterestRateCalculatorTest {

    private final TieredInterestRateCalculator calculator =
            new TieredInterestRateCalculator(TestLendingProperties.defaults());

    @Test
    void lowRiskSalariedSmallLoanIsBaseRate() {
        BigDecimal rate = calculator.calculate(RiskBand.LOW, EmploymentType.SALARIED, new BigDecimal("500000"));
        assertThat(rate).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    void mediumRiskSalariedSmallLoanAddsRiskPremium() {
        BigDecimal rate = calculator.calculate(RiskBand.MEDIUM, EmploymentType.SALARIED, new BigDecimal("500000"));
        assertThat(rate).isEqualByComparingTo(new BigDecimal("13.50"));
    }

    @Test
    void highRiskSelfEmployedLargeLoanStacksAllPremiums() {
        BigDecimal rate = calculator.calculate(RiskBand.HIGH, EmploymentType.SELF_EMPLOYED, new BigDecimal("1500000"));
        // 12 + 3 + 1 + 0.5 = 16.5
        assertThat(rate).isEqualByComparingTo(new BigDecimal("16.50"));
    }

    @Test
    void loanSizePremiumNotAppliedAtThresholdBoundary() {
        BigDecimal atThreshold = calculator.calculate(RiskBand.LOW, EmploymentType.SALARIED, new BigDecimal("1000000"));
        BigDecimal aboveThreshold = calculator.calculate(RiskBand.LOW, EmploymentType.SALARIED, new BigDecimal("1000001"));
        assertThat(atThreshold).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(aboveThreshold).isEqualByComparingTo(new BigDecimal("12.50"));
    }
}
