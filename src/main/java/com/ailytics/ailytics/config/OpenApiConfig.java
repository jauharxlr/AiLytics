package com.ailytics.ailytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AiLytics - AI-Powered Portal Bridge API")
                        .version("1.1.0")
                        .description("AiLytics is a next-generation automation system that bridges the gap between unstructured document data and legacy web portals. " +
                                     "It features multimodal extraction using **Gemini 2.0 Flash** and **Semantic Automation** via Playwright, " +
                                     "allowing for autonomous form-filling without fragile CSS selectors.")
                        .contact(new Contact()
                                .name("AiLytics Support")
                                .email("support@ailytics.example.com")));
    }
}
