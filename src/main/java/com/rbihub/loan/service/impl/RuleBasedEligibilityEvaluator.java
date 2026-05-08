package com.rbihub.loan.service.impl;

import com.rbihub.loan.config.LendingProperties;
import com.rbihub.loan.domain.enums.RejectionReason;
import com.rbihub.loan.service.EligibilityEvaluator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RuleBasedEligibilityEvaluator implements EligibilityEvaluator {

    private static final int MONTHS_PER_YEAR = 12;
    private static final MathContext MC = MathContext.DECIMAL64;

    private final LendingProperties.Eligibility rules;

    public RuleBasedEligibilityEvaluator(LendingProperties properties) {
        this.rules = properties.eligibility();
    }

    @Override
    public List<RejectionReason> evaluatePreEmi(int creditScore, int age, int tenureMonths) {
        List<RejectionReason> reasons = new ArrayList<>(2);
        if (creditScore < rules.minCreditScore()) {
            reasons.add(RejectionReason.LOW_CREDIT_SCORE);
        }
        if (age * MONTHS_PER_YEAR + tenureMonths > rules.maxAgePlusTenureYears() * MONTHS_PER_YEAR) {
            reasons.add(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        }
        return reasons.isEmpty() ? List.of() : Collections.unmodifiableList(reasons);
    }

    @Override
    public List<RejectionReason> evaluateEmiAffordability(BigDecimal emi, BigDecimal monthlyIncome) {
        BigDecimal ratio = emi.divide(monthlyIncome, MC);
        if (ratio.compareTo(rules.maxEmiToIncomeRatio()) > 0) {
            return List.of(RejectionReason.EMI_EXCEEDS_60_PERCENT);
        }
        if (ratio.compareTo(rules.maxOfferEmiToIncomeRatio()) > 0) {
            return List.of(RejectionReason.EMI_EXCEEDS_50_PERCENT);
        }
        return List.of();
    }
}
