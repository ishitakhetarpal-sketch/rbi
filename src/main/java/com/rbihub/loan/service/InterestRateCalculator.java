package com.rbihub.loan.service;

import com.rbihub.loan.domain.enums.EmploymentType;
import com.rbihub.loan.domain.enums.RiskBand;

import java.math.BigDecimal;

public interface InterestRateCalculator {

    /**
     * Computes the final annual interest rate (percentage) by adding all
     * applicable premiums to the configured base rate.
     */
    BigDecimal calculate(RiskBand riskBand, EmploymentType employmentType, BigDecimal loanAmount);
}
