package com.rbihub.loan.service;

import com.rbihub.loan.domain.enums.RejectionReason;

import java.math.BigDecimal;
import java.util.List;

public interface EligibilityEvaluator {

    /**
     * Checks every rule that does not depend on the EMI: credit score floor and
     * age-plus-tenure limit.
     */
    List<RejectionReason> evaluatePreEmi(int creditScore, int age, int tenureMonths);

    /**
     * Checks the EMI affordability rules. The 60% rule is the strict
     * eligibility cap; the 50% rule decides whether the generated offer is
     * actually issued. Only the strictest matching reason is returned so the
     * response stays focused.
     */
    List<RejectionReason> evaluateEmiAffordability(BigDecimal emi, BigDecimal monthlyIncome);
}
