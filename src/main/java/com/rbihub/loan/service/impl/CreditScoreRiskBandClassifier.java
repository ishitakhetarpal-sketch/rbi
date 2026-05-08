package com.rbihub.loan.service.impl;

import com.rbihub.loan.config.LendingProperties;
import com.rbihub.loan.domain.enums.RiskBand;
import com.rbihub.loan.service.RiskBandClassifier;
import org.springframework.stereotype.Component;

@Component
public class CreditScoreRiskBandClassifier implements RiskBandClassifier {

    private final LendingProperties.RiskBands thresholds;

    public CreditScoreRiskBandClassifier(LendingProperties properties) {
        this.thresholds = properties.riskBands();
    }

    @Override
    public RiskBand classify(int creditScore) {
        if (creditScore >= thresholds.lowMinScore()) {
            return RiskBand.LOW;
        }
        if (creditScore >= thresholds.mediumMinScore()) {
            return RiskBand.MEDIUM;
        }
        if (creditScore >= thresholds.highMinScore()) {
            return RiskBand.HIGH;
        }
        return null;
    }
}
