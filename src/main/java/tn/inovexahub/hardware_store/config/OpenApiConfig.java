package tn.inovexahub.hardware_store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI hardwareStoreOpenAPI() {
    SecurityScheme bearerAuthScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT token for authentication");

    Contact contact =
        new Contact()
            .name("INOVEXAHUB")
            .email("contact@inovexahub.tn")
            .url("https://inovexahub.tn");

    License license =
        new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

    Info info =
        new Info()
            .title("INOVEXAHUB Hardware Store API")
            .version("1.0.0")
            .description(
                "RESTful API for INOVEXAHUB Hardware Store POS System - "
                    + "A comprehensive point of sale system for Tunisian hardware stores "
                    + "with multi-unit products, credit management, and fiscal compliance.")
            .contact(contact)
            .license(license);

    return new OpenAPI()
        .info(info)
        .components(
            new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth", bearerAuthScheme));
  }
}
