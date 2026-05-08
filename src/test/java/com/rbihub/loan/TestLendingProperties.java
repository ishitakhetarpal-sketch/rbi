package com.rbihub.loan;

import com.rbihub.loan.config.LendingProperties;
import com.rbihub.loan.domain.enums.EmploymentType;
import com.rbihub.loan.domain.enums.RiskBand;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Provides the canonical {@link LendingProperties} configuration declared in the
 * assignment. Unit tests reuse this so they exercise the same numeric tables
 * that production code runs with.
 */
public final class TestLendingProperties {

    private TestLendingProperties() {
    }

    public static LendingProperties defaults() {
        return new LendingProperties(
                new BigDecimal("12.00"),
                Map.of(
                        RiskBand.LOW, new BigDecimal("0.00"),
                        RiskBand.MEDIUM, new BigDecimal("1.50"),
                        RiskBand.HIGH, new BigDecimal("3.00")
                ),
                Map.of(
                        EmploymentType.SALARIED, new BigDecimal("0.00"),
                        EmploymentType.SELF_EMPLOYED, new BigDecimal("1.00")
                ),
                new LendingProperties.LoanSize(new BigDecimal("1000000"), new BigDecimal("0.50")),
                new LendingProperties.Eligibility(600, 65, new BigDecimal("0.60"), new BigDecimal("0.50")),
                new LendingProperties.RiskBands(750, 650, 600)
        );
    }
}
