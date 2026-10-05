package com.ichat.authbe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authAppOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Auth App API")
                .version("v1")
                .description(
                        "Login, registration, Google/Facebook sign-in, and forgot/reset password. "
                        + "See the accompanying documentation bundle for account-linking behavior, "
                        + "the data model, and OAuth/SMS provider setup."
                ));
    }
}
