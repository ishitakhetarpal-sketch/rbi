package com.rbihub.loan.service;

import com.rbihub.loan.service.impl.StandardEmiCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandardEmiCalculatorTest {

    private final StandardEmiCalculator calculator = new StandardEmiCalculator();

    @Nested
    @DisplayName("happy paths")
    class HappyPaths {

        @Test
        @DisplayName("computes the textbook EMI for 1L @ 12% over 12 months")
        void textbookOneLakh() {
            BigDecimal emi = calculator.calculate(new BigDecimal("100000"), new BigDecimal("12"), 12);
            assertThat(emi).isEqualByComparingTo(new BigDecimal("8884.88"));
        }

        @Test
        @DisplayName("returns scale=2 BigDecimal regardless of input scale")
        void resultIsAlwaysScaleTwo() {
            BigDecimal emi = calculator.calculate(new BigDecimal("500000"), new BigDecimal("13.5"), 36);
            assertThat(emi.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("0% interest splits the principal evenly across the tenure")
        void zeroInterestRate() {
            BigDecimal emi = calculator.calculate(new BigDecimal("120000"), BigDecimal.ZERO, 12);
            assertThat(emi).isEqualByComparingTo(new BigDecimal("10000.00"));
        }

        @Test
        @DisplayName("computes EMI for 5L @ 13.5% over 36 months")
        void textbookFiveLakhs() {
            BigDecimal emi = calculator.calculate(new BigDecimal("500000"), new BigDecimal("13.5"), 36);
            assertThat(emi).isEqualByComparingTo(new BigDecimal("16967.64"));
        }

        @Test
        @DisplayName("a higher interest rate produces a higher EMI for the same principal/tenure")
        void higherRateHigherEmi() {
            BigDecimal lower = calculator.calculate(new BigDecimal("500000"), new BigDecimal("12"), 36);
            BigDecimal higher = calculator.calculate(new BigDecimal("500000"), new BigDecimal("13.5"), 36);
            assertThat(higher).isGreaterThan(lower);
        }
    }

    @Nested
    @DisplayName("input validation")
    class InputValidation {

        @Test
        void principalMustBePositive() {
            assertThatThrownBy(() -> calculator.calculate(BigDecimal.ZERO, new BigDecimal("12"), 12))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rateMustNotBeNegative() {
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal("100000"), new BigDecimal("-1"), 12))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void tenureMustBePositive() {
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal("100000"), new BigDecimal("12"), 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
