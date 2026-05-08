package com.rbihub.loan.domain.model;

import com.rbihub.loan.domain.enums.ApplicationStatus;
import com.rbihub.loan.domain.enums.RejectionReason;
import com.rbihub.loan.domain.enums.RiskBand;

import java.util.List;
import java.util.UUID;

public record Decision(
        UUID applicationId,
        ApplicationStatus status,
        RiskBand riskBand,
        Offer offer,
        List<RejectionReason> rejectionReasons
) {
    public static Decision approved(UUID id, RiskBand band, Offer offer) {
        return new Decision(id, ApplicationStatus.APPROVED, band, offer, List.of());
    }

    public static Decision rejected(UUID id, List<RejectionReason> reasons) {
        return new Decision(id, ApplicationStatus.REJECTED, null, null, List.copyOf(reasons));
    }
}
