package com.example.amhs.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI amhsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AMHS API")
                        .description("AMHS transfer job routing and state management API")
                        .version("v0.0.1")
                        .contact(new Contact().name("hyemin")));
    }
}
