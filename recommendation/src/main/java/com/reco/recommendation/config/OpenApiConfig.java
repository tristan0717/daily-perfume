package com.reco.recommendation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI perfumeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Perfume Recommendation API")
                        .description("향수 추천 서비스 API 명세서입니다.")
                        .version("v1.0.0"));
    }
}