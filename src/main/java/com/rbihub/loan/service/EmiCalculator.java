package com.rbihub.loan.service;

import java.math.BigDecimal;

/**
 * Computes the equated monthly instalment (EMI) for a loan.
 */
public interface EmiCalculator {

    /**
     * @param principal      loan principal (rupees)
     * @param annualRatePct  annual interest rate as a percentage (e.g. {@code 13.5})
     * @param tenureMonths   number of monthly instalments
     * @return EMI rounded to 2 decimals using {@code HALF_UP}
     */
    BigDecimal calculate(BigDecimal principal, BigDecimal annualRatePct, int tenureMonths);
}
