package com.rbihub.loan.service;

import com.rbihub.loan.domain.enums.RiskBand;

public interface RiskBandClassifier {

    /**
     * Classifies an applicant's credit score into a risk band.
     *
     * @param creditScore validated credit score in the range 300–900
     * @return the corresponding {@link RiskBand}, or {@code null} if the score
     * does not qualify for any band (i.e. below the lowest configured threshold)
     */
    RiskBand classify(int creditScore);
}
