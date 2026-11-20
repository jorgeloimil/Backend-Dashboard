package com.homedashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Publica el contrato fuente para que Swagger UI (/swagger-ui.html) lo muestre tal cual. */
@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-file/**")
                .addResourceLocations("classpath:/swagger-file/");
    }
}
