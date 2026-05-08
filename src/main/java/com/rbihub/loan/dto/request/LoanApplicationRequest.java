package com.rbihub.loan.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record LoanApplicationRequest(
        @NotNull(message = "applicant is required")
        @Valid ApplicantDto applicant,

        @NotNull(message = "loan is required")
        @Valid LoanDto loan
) {
}
