package tn.inovexahub.hardware_store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
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

    Info info =
        new Info()
            .title("INOVEXAHUB Hardware Store API")
            .version("1.0.0")
            .description(
                "RESTful API for INOVEXAHUB Hardware Store POS System - "
                    + "A comprehensive point of sale system for Tunisian hardware stores "
                    + "with multi-unit products, credit management, and fiscal compliance.");

    Tag authTag =
        new Tag()
            .name("Authentication")
            .description("Authentication and user management endpoints");
    Tag clientTag =
        new Tag()
            .name("Clients")
            .description("Client management including credit balances and payment tracking");
    Tag documentTag =
        new Tag()
            .name("Documents")
            .description(
                "Document management including quotes, delivery notes, invoices, and PDF export");
    Tag productTag =
        new Tag()
            .name("Products")
            .description(
                "Product management including variants (multi-SKU with flexible JSON attributes), "
                    + "multi-unit conditioning, and purchase costs");
    Tag reportingTag =
        new Tag()
            .name("Reporting")
            .description(
                "Analytics, revenue tracking, margin calculation, and CSV exports (Admin only)");
    Tag supplierTag =
        new Tag().name("Suppliers").description("Supplier contact and fiscal profile management");

    return new OpenAPI()
        .info(info)
        .tags(List.of(authTag, clientTag, documentTag, productTag, reportingTag, supplierTag))
        .components(
            new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth", bearerAuthScheme));
  }
}
