package ng.lendstack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lendstackOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("LendStack API")
                .version("v1")
                .description("""
                    Loan Management System for the Nigerian consumer-lending market.

                    **Business context:** individual personal loans with full lifecycle \
                    (application → review → credit check → guarantors → collateral → approval → \
                    disbursement → repayment → closure), CBN-aware compliance (interest rate cap, \
                    1–24 month tenure, BVN linkage, reducing-balance amortization), multi-lender \
                    funding, and Paystack repayments. All monetary values are Naira (NGN) with \
                    2 decimal places. All list endpoints are paginated (`page`, `size`). Every \
                    response uses the envelope `{ success, data, message, timestamp }`.

                    **Roles:** `/borrower/**` = BORROWER, `/officer/**` = LOAN_OFFICER, \
                    `/admin/**` = ADMIN. Obtain a JWT from `/api/v1/auth/login` and click \
                    Authorize to use it here."""))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
