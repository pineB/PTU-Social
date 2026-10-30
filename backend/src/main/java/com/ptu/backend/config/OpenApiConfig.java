package com.ptu.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ptuOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("PTU Backend API")
                .description("Academic module API for tasks, submissions and academic ratings")
                .version("1.0.0"));
    }
}
