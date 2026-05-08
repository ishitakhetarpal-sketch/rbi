package com.rbihub.loan.dto.request;

import com.rbihub.loan.domain.enums.EmploymentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ApplicantDto(
        @NotBlank(message = "applicant.name must not be blank")
        String name,

        @Min(value = 21, message = "applicant.age must be between 21 and 60")
        @Max(value = 60, message = "applicant.age must be between 21 and 60")
        int age,

        @NotNull(message = "applicant.monthlyIncome is required")
        @Positive(message = "applicant.monthlyIncome must be greater than 0")
        BigDecimal monthlyIncome,

        @NotNull(message = "applicant.employmentType is required")
        EmploymentType employmentType,

        @Min(value = 300, message = "applicant.creditScore must be between 300 and 900")
        @Max(value = 900, message = "applicant.creditScore must be between 300 and 900")
        int creditScore
) {
}
