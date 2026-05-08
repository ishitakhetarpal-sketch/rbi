package com.rbihub.loan.service.impl;

import com.rbihub.loan.service.EmiCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Standard reducing-balance EMI implementation.
 *
 * <pre>
 *     EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
 * </pre>
 *
 * where {@code r} is the monthly interest rate and {@code n} is the tenure in months.
 * Intermediate calculations use {@link MathContext#DECIMAL64} so that compounding
 * remains accurate; the final value is rounded to scale 2, {@code HALF_UP}.
 */
@Component
public class StandardEmiCalculator implements EmiCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    @Override
    public BigDecimal calculate(BigDecimal principal, BigDecimal annualRatePct, int tenureMonths) {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(annualRatePct, "annualRatePct must not be null");
        if (principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive");
        }
        if (annualRatePct.signum() < 0) {
            throw new IllegalArgumentException("annualRatePct must not be negative");
        }
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("tenureMonths must be positive");
        }

        BigDecimal monthlyRate = annualRatePct.divide(HUNDRED, MC).divide(TWELVE, MC);

        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusRtoN = BigDecimal.ONE.add(monthlyRate, MC).pow(tenureMonths, MC);
        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(onePlusRtoN, MC);
        BigDecimal denominator = onePlusRtoN.subtract(BigDecimal.ONE, MC);
        return numerator.divide(denominator, MC).setScale(2, RoundingMode.HALF_UP);
    }
}
