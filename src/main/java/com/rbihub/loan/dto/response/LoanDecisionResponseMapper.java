package com.rbihub.loan.dto.response;

import com.rbihub.loan.domain.model.Decision;
import com.rbihub.loan.domain.model.Offer;

import java.util.List;

/**
 * Maps the immutable {@link Decision} domain object into the API response DTO.
 * Kept as a static helper because the mapping has no collaborators.
 */
public final class LoanDecisionResponseMapper {

    private LoanDecisionResponseMapper() {
    }

    public static LoanDecisionResponse toResponse(Decision decision) {
        OfferDto offerDto = toOfferDto(decision.offer());
        List<String> reasons = decision.rejectionReasons().isEmpty()
                ? null
                : decision.rejectionReasons().stream().map(Enum::name).toList();
        return new LoanDecisionResponse(
                decision.applicationId(),
                decision.status(),
                decision.riskBand(),
                offerDto,
                reasons
        );
    }

    private static OfferDto toOfferDto(Offer offer) {
        if (offer == null) {
            return null;
        }
        return new OfferDto(offer.interestRate(), offer.tenureMonths(), offer.emi(), offer.totalPayable());
    }
}
