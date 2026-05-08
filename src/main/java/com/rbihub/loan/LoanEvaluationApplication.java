package com.rbihub.loan;

import com.rbihub.loan.config.LendingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(LendingProperties.class)
public class LoanEvaluationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanEvaluationApplication.class, args);
    }
}
