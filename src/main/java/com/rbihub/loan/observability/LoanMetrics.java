package com.rbihub.loan.observability;

import com.rbihub.loan.domain.enums.RejectionReason;
import com.rbihub.loan.domain.enums.RiskBand;
import com.rbihub.loan.domain.model.Decision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Records application-level decision metrics so dashboards and alerts can
 * track approval rates, rejection breakdowns and risk-band distribution
 * without parsing logs.
 */
@Component
public class LoanMetrics {

    private static final String DECISIONS = "loan.decisions";
    private static final String REJECTIONS = "loan.rejections";
    private static final String AMOUNTS = "loan.amount";
    private static final String EMI_RATIO = "loan.emi.ratio";

    private final MeterRegistry registry;

    public LoanMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(Decision decision, BigDecimal loanAmount, BigDecimal monthlyIncome) {
        Counter.builder(DECISIONS)
                .description("Number of loan-application decisions, tagged by status and risk band")
                .tag("status", decision.status().name())
                .tag("riskBand", decision.riskBand() == null ? "NONE" : decision.riskBand().name())
                .register(registry)
                .increment();

        for (RejectionReason reason : decision.rejectionReasons()) {
            Counter.builder(REJECTIONS)
                    .description("Rejections broken down by reason")
                    .tag("reason", reason.name())
                    .register(registry)
                    .increment();
        }

        DistributionSummary.builder(AMOUNTS)
                .description("Distribution of requested loan amounts (INR)")
                .baseUnit("INR")
                .tag("status", decision.status().name())
                .register(registry)
                .record(loanAmount.doubleValue());

        if (decision.offer() != null && monthlyIncome.signum() > 0) {
            BigDecimal ratio = decision.offer().emi().divide(monthlyIncome, java.math.MathContext.DECIMAL64);
            DistributionSummary.builder(EMI_RATIO)
                    .description("Distribution of EMI / monthly-income for issued offers")
                    .tag("riskBand", decision.riskBand() == null ? "NONE" : decision.riskBand().name())
                    .register(registry)
                    .record(ratio.doubleValue());
        }
    }

    public void incrementRiskBandClassified(RiskBand band) {
        Counter.builder("loan.risk.classified")
                .tags(Tags.of("band", band == null ? "NONE" : band.name()))
                .register(registry)
                .increment();
    }
}
