package com.rbihub.loan.service;

import com.rbihub.loan.TestLendingProperties;
import com.rbihub.loan.domain.enums.ApplicationStatus;
import com.rbihub.loan.domain.enums.EmploymentType;
import com.rbihub.loan.domain.enums.LoanPurpose;
import com.rbihub.loan.domain.enums.RejectionReason;
import com.rbihub.loan.domain.enums.RiskBand;
import com.rbihub.loan.domain.model.Decision;
import com.rbihub.loan.domain.model.LoanApplicationRecord;
import com.rbihub.loan.dto.request.ApplicantDto;
import com.rbihub.loan.dto.request.LoanApplicationRequest;
import com.rbihub.loan.dto.request.LoanDto;
import com.rbihub.loan.repository.LoanApplicationRepository;
import com.rbihub.loan.service.impl.CreditScoreRiskBandClassifier;
import com.rbihub.loan.service.impl.DefaultLoanEvaluationService;
import com.rbihub.loan.service.impl.RuleBasedEligibilityEvaluator;
import com.rbihub.loan.service.impl.StandardEmiCalculator;
import com.rbihub.loan.service.impl.TieredInterestRateCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLoanEvaluationServiceTest {

    private LoanApplicationRepository repository;
    private DefaultLoanEvaluationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LoanApplicationRepository.class);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var props = TestLendingProperties.defaults();
        service = new DefaultLoanEvaluationService(
                new RuleBasedEligibilityEvaluator(props),
                new CreditScoreRiskBandClassifier(props),
                new TieredInterestRateCalculator(props),
                new StandardEmiCalculator(),
                repository
        );
    }

    @Test
    void approvesEligibleApplication() {
        LoanApplicationRequest req = new LoanApplicationRequest(
                new ApplicantDto("Asha", 30, new BigDecimal("75000"),
                        EmploymentType.SALARIED, 780),
                new LoanDto(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(req);

        assertThat(decision.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(decision.riskBand()).isEqualTo(RiskBand.LOW);
        assertThat(decision.offer()).isNotNull();
        assertThat(decision.offer().tenureMonths()).isEqualTo(36);
        assertThat(decision.offer().interestRate()).isEqualByComparingTo("12.00");
        assertThat(decision.offer().emi()).isPositive();
        assertThat(decision.offer().totalPayable())
                .isEqualByComparingTo(decision.offer().emi().multiply(new BigDecimal("36")));
        verify(repository, atLeastOnce()).save(any(LoanApplicationRecord.class));
    }

    @Test
    void rejectsLowCreditScore() {
        LoanApplicationRequest req = new LoanApplicationRequest(
                new ApplicantDto("Bob", 30, new BigDecimal("75000"),
                        EmploymentType.SALARIED, 580),
                new LoanDto(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(req);

        assertThat(decision.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(decision.riskBand()).isNull();
        assertThat(decision.offer()).isNull();
        assertThat(decision.rejectionReasons()).contains(RejectionReason.LOW_CREDIT_SCORE);
    }

    @Test
    void rejectsWhenEmiExceedsSixtyPercent() {
        // Tiny income relative to loan -> EMI will dominate income
        LoanApplicationRequest req = new LoanApplicationRequest(
                new ApplicantDto("Cara", 30, new BigDecimal("20000"),
                        EmploymentType.SALARIED, 780),
                new LoanDto(new BigDecimal("500000"), 36, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(req);

        assertThat(decision.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(decision.rejectionReasons()).contains(RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }

    @Test
    void rejectsWhenEmiBetweenFiftyAndSixtyPercent() {
        // 5L @ ~12% over 60 months -> EMI ~ 11122
        // monthly income 20500 -> ratio ~ 0.54  (between 50% and 60%)
        LoanApplicationRequest req = new LoanApplicationRequest(
                new ApplicantDto("Dev", 30, new BigDecimal("20500"),
                        EmploymentType.SALARIED, 780),
                new LoanDto(new BigDecimal("500000"), 60, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(req);

        assertThat(decision.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(decision.rejectionReasons()).contains(RejectionReason.EMI_EXCEEDS_50_PERCENT);
    }

    @Test
    void accumulatesPreEmiAndEmiReasons() {
        // age+tenure=66 (rejected) AND tiny income -> EMI > 60%
        LoanApplicationRequest req = new LoanApplicationRequest(
                new ApplicantDto("Eli", 60, new BigDecimal("5000"),
                        EmploymentType.SALARIED, 780),
                new LoanDto(new BigDecimal("500000"), 84, LoanPurpose.PERSONAL)
        );

        Decision decision = service.evaluate(req);

        assertThat(decision.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(decision.rejectionReasons())
                .contains(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED,
                          RejectionReason.EMI_EXCEEDS_60_PERCENT);
    }
}
