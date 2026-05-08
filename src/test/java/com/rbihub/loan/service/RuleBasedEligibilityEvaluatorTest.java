package com.rbihub.loan.service;

import com.rbihub.loan.TestLendingProperties;
import com.rbihub.loan.domain.enums.RejectionReason;
import com.rbihub.loan.service.impl.RuleBasedEligibilityEvaluator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedEligibilityEvaluatorTest {

    private final RuleBasedEligibilityEvaluator evaluator =
            new RuleBasedEligibilityEvaluator(TestLendingProperties.defaults());

    @Nested
    class PreEmiChecks {

        @Test
        void eligibleApplicantHasNoReasons() {
            assertThat(evaluator.evaluatePreEmi(720, 30, 36)).isEmpty();
        }

        @Test
        void belowMinimumCreditScoreIsRejected() {
            assertThat(evaluator.evaluatePreEmi(599, 30, 36))
                    .containsExactly(RejectionReason.LOW_CREDIT_SCORE);
        }

        @Test
        void agePlusTenureExceedingLimitIsRejected() {
            // 60 + 84/12 = 67
            assertThat(evaluator.evaluatePreEmi(720, 60, 84))
                    .containsExactly(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        }

        @Test
        void agePlusTenureAtBoundaryIsAccepted() {
            // 60 + 60/12 = 65 -> exactly at limit, NOT exceeding
            assertThat(evaluator.evaluatePreEmi(720, 60, 60)).isEmpty();
        }

        @Test
        void multipleViolationsAreReportedTogether() {
            assertThat(evaluator.evaluatePreEmi(550, 60, 84))
                    .containsExactlyInAnyOrder(
                            RejectionReason.LOW_CREDIT_SCORE,
                            RejectionReason.AGE_TENURE_LIMIT_EXCEEDED);
        }
    }

    @Nested
    class EmiAffordabilityChecks {

        @Test
        void emiBelowFiftyPercentIsAccepted() {
            assertThat(evaluator.evaluateEmiAffordability(new BigDecimal("30000"), new BigDecimal("75000")))
                    .isEmpty();
        }

        @Test
        void emiBetweenFiftyAndSixtyPercentTriggersOfferReject() {
            // 42000 / 75000 = 0.56  (between 50% and 60%)
            assertThat(evaluator.evaluateEmiAffordability(new BigDecimal("42000"), new BigDecimal("75000")))
                    .containsExactly(RejectionReason.EMI_EXCEEDS_50_PERCENT);
        }

        @Test
        void emiAboveSixtyPercentTriggersEligibilityReject() {
            // 50000 / 75000 = 0.66
            assertThat(evaluator.evaluateEmiAffordability(new BigDecimal("50000"), new BigDecimal("75000")))
                    .containsExactly(RejectionReason.EMI_EXCEEDS_60_PERCENT);
        }

        @Test
        void emiExactlyAtFiftyPercentBoundaryIsAccepted() {
            assertThat(evaluator.evaluateEmiAffordability(new BigDecimal("37500"), new BigDecimal("75000")))
                    .isEmpty();
        }
    }
}
