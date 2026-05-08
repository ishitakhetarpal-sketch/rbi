package com.rbihub.loan.service.impl;

import com.rbihub.loan.domain.enums.RejectionReason;
import com.rbihub.loan.domain.enums.RiskBand;
import com.rbihub.loan.domain.model.Decision;
import com.rbihub.loan.domain.model.LoanApplicationRecord;
import com.rbihub.loan.domain.model.Offer;
import com.rbihub.loan.dto.request.ApplicantDto;
import com.rbihub.loan.dto.request.LoanApplicationRequest;
import com.rbihub.loan.dto.request.LoanDto;
import com.rbihub.loan.repository.LoanApplicationRepository;
import com.rbihub.loan.service.EligibilityEvaluator;
import com.rbihub.loan.service.EmiCalculator;
import com.rbihub.loan.service.InterestRateCalculator;
import com.rbihub.loan.service.LoanEvaluationService;
import com.rbihub.loan.service.RiskBandClassifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the loan-evaluation pipeline by composing focused collaborators:
 * eligibility rules, risk-band classification, interest-rate calculation and
 * EMI computation. The orchestrator owns no business rules of its own.
 */
@Service
public class DefaultLoanEvaluationService implements LoanEvaluationService {

    private final EligibilityEvaluator eligibilityEvaluator;
    private final RiskBandClassifier riskBandClassifier;
    private final InterestRateCalculator interestRateCalculator;
    private final EmiCalculator emiCalculator;
    private final LoanApplicationRepository repository;

    public DefaultLoanEvaluationService(EligibilityEvaluator eligibilityEvaluator,
                                        RiskBandClassifier riskBandClassifier,
                                        InterestRateCalculator interestRateCalculator,
                                        EmiCalculator emiCalculator,
                                        LoanApplicationRepository repository) {
        this.eligibilityEvaluator = eligibilityEvaluator;
        this.riskBandClassifier = riskBandClassifier;
        this.interestRateCalculator = interestRateCalculator;
        this.emiCalculator = emiCalculator;
        this.repository = repository;
    }

    @Override
    @Transactional
    public Decision evaluate(LoanApplicationRequest request) {
        UUID applicationId = UUID.randomUUID();
        ApplicantDto applicant = request.applicant();
        LoanDto loan = request.loan();

        List<RejectionReason> reasons = new ArrayList<>(
                eligibilityEvaluator.evaluatePreEmi(
                        applicant.creditScore(), applicant.age(), loan.tenureMonths()));

        if (reasons.contains(RejectionReason.LOW_CREDIT_SCORE)) {
            return persist(applicationId, request, Decision.rejected(applicationId, reasons), null, null, null, null);
        }

        RiskBand band = riskBandClassifier.classify(applicant.creditScore());
        BigDecimal rate = interestRateCalculator.calculate(band, applicant.employmentType(), loan.amount());
        BigDecimal emi = emiCalculator.calculate(loan.amount(), rate, loan.tenureMonths());

        reasons.addAll(eligibilityEvaluator.evaluateEmiAffordability(emi, applicant.monthlyIncome()));

        if (!reasons.isEmpty()) {
            return persist(applicationId, request, Decision.rejected(applicationId, reasons), band, rate, emi, null);
        }

        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(loan.tenureMonths()))
                .setScale(2, RoundingMode.HALF_UP);
        Offer offer = new Offer(rate, loan.tenureMonths(), emi, totalPayable);
        Decision decision = Decision.approved(applicationId, band, offer);
        return persist(applicationId, request, decision, band, rate, emi, totalPayable);
    }

    private Decision persist(UUID applicationId,
                             LoanApplicationRequest request,
                             Decision decision,
                             RiskBand band,
                             BigDecimal rate,
                             BigDecimal emi,
                             BigDecimal totalPayable) {
        ApplicantDto applicant = request.applicant();
        LoanDto loan = request.loan();
        String rejectionCsv = decision.rejectionReasons().isEmpty()
                ? null
                : decision.rejectionReasons().stream().map(Enum::name).collect(Collectors.joining(","));

        LoanApplicationRecord record = new LoanApplicationRecord(
                applicationId,
                applicant.name(),
                applicant.age(),
                applicant.monthlyIncome(),
                applicant.employmentType(),
                applicant.creditScore(),
                loan.amount(),
                loan.tenureMonths(),
                loan.purpose(),
                decision.status(),
                band,
                rate,
                emi,
                totalPayable,
                rejectionCsv
        );
        repository.save(record);
        return decision;
    }
}
