package com.rbihub.loan.repository;

import com.rbihub.loan.domain.model.LoanApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplicationRecord, UUID> {
}
