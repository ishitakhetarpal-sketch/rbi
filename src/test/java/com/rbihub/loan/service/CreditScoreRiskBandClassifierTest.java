package com.rbihub.loan.service;

import com.rbihub.loan.TestLendingProperties;
import com.rbihub.loan.domain.enums.RiskBand;
import com.rbihub.loan.service.impl.CreditScoreRiskBandClassifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CreditScoreRiskBandClassifierTest {

    private final CreditScoreRiskBandClassifier classifier =
            new CreditScoreRiskBandClassifier(TestLendingProperties.defaults());

    @ParameterizedTest(name = "score={0} -> {1}")
    @CsvSource({
            "900, LOW",
            "750, LOW",
            "749, MEDIUM",
            "700, MEDIUM",
            "650, MEDIUM",
            "649, HIGH",
            "600, HIGH"
    })
    void classifiesByScore(int score, RiskBand expected) {
        assertThat(classifier.classify(score)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "score={0} -> null")
    @CsvSource({"599", "500", "300"})
    void scoresBelowFloorHaveNoBand(int score) {
        assertThat(classifier.classify(score)).isNull();
    }
}
