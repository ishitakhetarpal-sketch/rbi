package com.rbihub.loan.controller;

import com.rbihub.loan.domain.model.Decision;
import com.rbihub.loan.dto.request.LoanApplicationRequest;
import com.rbihub.loan.dto.response.ErrorResponse;
import com.rbihub.loan.dto.response.LoanDecisionResponse;
import com.rbihub.loan.dto.response.LoanDecisionResponseMapper;
import com.rbihub.loan.service.LoanEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
@Tag(name = "Loan Applications",
     description = "Submit loan applications and receive eligibility decisions with offers.")
public class LoanApplicationController {

    private final LoanEvaluationService loanEvaluationService;

    public LoanApplicationController(LoanEvaluationService loanEvaluationService) {
        this.loanEvaluationService = loanEvaluationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Evaluate a loan application",
            description = "Validates the request, runs eligibility rules, classifies the credit risk band, "
                    + "computes the interest rate and EMI for the requested tenure and returns either an "
                    + "approved offer or a rejection with reasons. The decision is stored for audit."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Decision generated. Inspect `status` to determine APPROVED vs REJECTED.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoanDecisionResponse.class),
                            examples = {
                                    @ExampleObject(name = "Approved", value = APPROVED_EXAMPLE),
                                    @ExampleObject(name = "Rejected", value = REJECTED_EXAMPLE)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation or parsing error.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<LoanDecisionResponse> create(
            @RequestBody
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoanApplicationRequest.class),
                            examples = @ExampleObject(name = "Sample request", value = REQUEST_EXAMPLE)
                    )
            )
            LoanApplicationRequest request) {
        Decision decision = loanEvaluationService.evaluate(request);
        LoanDecisionResponse body = LoanDecisionResponseMapper.toResponse(decision);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private static final String REQUEST_EXAMPLE = """
            {
              "applicant": {
                "name": "Asha",
                "age": 30,
                "monthlyIncome": 75000,
                "employmentType": "SALARIED",
                "creditScore": 780
              },
              "loan": {
                "amount": 500000,
                "tenureMonths": 36,
                "purpose": "PERSONAL"
              }
            }
            """;

    private static final String APPROVED_EXAMPLE = """
            {
              "applicationId": "f3b6c7d2-2c2d-4d96-9f54-2a1aa2b71f31",
              "status": "APPROVED",
              "riskBand": "LOW",
              "offer": {
                "interestRate": 12.00,
                "tenureMonths": 36,
                "emi": 16607.15,
                "totalPayable": 597857.40
              }
            }
            """;

    private static final String REJECTED_EXAMPLE = """
            {
              "applicationId": "f3b6c7d2-2c2d-4d96-9f54-2a1aa2b71f31",
              "status": "REJECTED",
              "rejectionReasons": ["EMI_EXCEEDS_60_PERCENT", "AGE_TENURE_LIMIT_EXCEEDED"]
            }
            """;
}
