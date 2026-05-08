package com.rbihub.loan.domain.model;

import com.rbihub.loan.domain.enums.ApplicationStatus;
import com.rbihub.loan.domain.enums.EmploymentType;
import com.rbihub.loan.domain.enums.LoanPurpose;
import com.rbihub.loan.domain.enums.RiskBand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityListeners;

@Entity
@Table(name = "loan_application_record")
@EntityListeners(AuditingEntityListener.class)
public class LoanApplicationRecord {

    @Id
    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "monthly_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Column(name = "credit_score", nullable = false)
    private int creditScore;

    @Column(name = "loan_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_purpose", nullable = false)
    private LoanPurpose loanPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band")
    private RiskBand riskBand;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "emi", precision = 19, scale = 2)
    private BigDecimal emi;

    @Column(name = "total_payable", precision = 19, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "rejection_reasons", length = 512)
    private String rejectionReasons;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LoanApplicationRecord() {
    }

    public LoanApplicationRecord(UUID applicationId,
                                 String applicantName,
                                 int age,
                                 BigDecimal monthlyIncome,
                                 EmploymentType employmentType,
                                 int creditScore,
                                 BigDecimal loanAmount,
                                 int tenureMonths,
                                 LoanPurpose loanPurpose,
                                 ApplicationStatus status,
                                 RiskBand riskBand,
                                 BigDecimal interestRate,
                                 BigDecimal emi,
                                 BigDecimal totalPayable,
                                 String rejectionReasons) {
        this.applicationId = applicationId;
        this.applicantName = applicantName;
        this.age = age;
        this.monthlyIncome = monthlyIncome;
        this.employmentType = employmentType;
        this.creditScore = creditScore;
        this.loanAmount = loanAmount;
        this.tenureMonths = tenureMonths;
        this.loanPurpose = loanPurpose;
        this.status = status;
        this.riskBand = riskBand;
        this.interestRate = interestRate;
        this.emi = emi;
        this.totalPayable = totalPayable;
        this.rejectionReasons = rejectionReasons;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public int getAge() {
        return age;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public LoanPurpose getLoanPurpose() {
        return loanPurpose;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public RiskBand getRiskBand() {
        return riskBand;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public BigDecimal getEmi() {
        return emi;
    }

    public BigDecimal getTotalPayable() {
        return totalPayable;
    }

    public String getRejectionReasons() {
        return rejectionReasons;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
