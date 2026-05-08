package com.rbihub.loan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void approvesAValidApplication() throws Exception {
        Map<String, Object> body = Map.of(
                "applicant", Map.of(
                        "name", "Asha",
                        "age", 30,
                        "monthlyIncome", 75000,
                        "employmentType", "SALARIED",
                        "creditScore", 780),
                "loan", Map.of(
                        "amount", 500000,
                        "tenureMonths", 36,
                        "purpose", "PERSONAL")
        );

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").exists())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.riskBand").value("LOW"))
                .andExpect(jsonPath("$.offer.tenureMonths").value(36))
                .andExpect(jsonPath("$.offer.interestRate").value(12.00))
                .andExpect(jsonPath("$.offer.emi").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.offer.totalPayable").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.rejectionReasons").doesNotExist());
    }

    @Test
    void rejectsLowCreditScore() throws Exception {
        Map<String, Object> body = Map.of(
                "applicant", Map.of(
                        "name", "Bob",
                        "age", 30,
                        "monthlyIncome", 75000,
                        "employmentType", "SALARIED",
                        "creditScore", 580),
                "loan", Map.of(
                        "amount", 500000,
                        "tenureMonths", 36,
                        "purpose", "PERSONAL")
        );

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.riskBand").doesNotExist())
                .andExpect(jsonPath("$.offer").doesNotExist())
                .andExpect(jsonPath("$.rejectionReasons[0]").value("LOW_CREDIT_SCORE"));
    }

    @Test
    void returnsBadRequestForFieldOutOfRange() throws Exception {
        Map<String, Object> body = Map.of(
                "applicant", Map.of(
                        "name", "Out Of Range",
                        "age", 18,
                        "monthlyIncome", 75000,
                        "employmentType", "SALARIED",
                        "creditScore", 780),
                "loan", Map.of(
                        "amount", 500000,
                        "tenureMonths", 36,
                        "purpose", "PERSONAL")
        );

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());
    }

    @Test
    void returnsBadRequestForUnknownEnumValue() throws Exception {
        String body = """
                {
                  "applicant": {
                    "name": "X",
                    "age": 30,
                    "monthlyIncome": 75000,
                    "employmentType": "ASTRONAUT",
                    "creditScore": 780
                  },
                  "loan": { "amount": 500000, "tenureMonths": 36, "purpose": "PERSONAL" }
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
