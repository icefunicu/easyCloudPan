package com.easypan.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EasyCloudPan API")
                        .version("1.0.0")
                        .description("RESTful API documentation for EasyCloudPan")
                        .contact(new Contact()
                                .name("EasyCloudPan Team")
                                .email("support@easypan.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Documentation")
                        .url("https://github.com/easypan/docs"));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("User Module")
                .pathsToMatch("/account/**", "/userInfo/**")
                .build();
    }

    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("File Module")
                .pathsToMatch("/file/**")
                .build();
    }

    @Bean
    public GroupedOpenApi shareApi() {
        return GroupedOpenApi.builder()
                .group("Share Module")
                .pathsToMatch("/share/**", "/showShare/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("Admin Module")
                .pathsToMatch("/admin/**")
                .build();
    }
}
