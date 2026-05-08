package com.rbihub.loan.service;

import com.rbihub.loan.domain.model.Decision;
import com.rbihub.loan.dto.request.LoanApplicationRequest;

public interface LoanEvaluationService {

    /**
     * Evaluates a loan application against eligibility rules, generates a single
     * offer when eligible, and persists the decision for audit.
     */
    Decision evaluate(LoanApplicationRequest request);
}
