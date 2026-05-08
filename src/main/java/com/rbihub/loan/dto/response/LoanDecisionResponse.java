package com.rbihub.loan.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rbihub.loan.domain.enums.ApplicationStatus;
import com.rbihub.loan.domain.enums.RiskBand;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoanDecisionResponse(
        UUID applicationId,
        ApplicationStatus status,
        RiskBand riskBand,
        OfferDto offer,
        List<String> rejectionReasons
) {
}
