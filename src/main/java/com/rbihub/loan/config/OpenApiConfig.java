package com.rbihub.loan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI loanEvaluationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loan Evaluation Service API")
                        .description("REST API that evaluates loan applications against eligibility "
                                + "rules and generates a single offer based on the requested tenure.")
                        .version("v1")
                        .contact(new Contact().name("RBI Hub").url("https://rbihub.in"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
