package com.maggu.maggu.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("maggu API")
                        .description("여행 사진 커뮤니티 서비스 API 문서. 모든 응답은 공통 포맷(ApiResponse)으로 감싸져 내려가며, "
                                + "실패 응답의 code는 도메인 접두사(USER-xxx, POST-xxx 등) 또는 COMMON-xxx로 구분된다.")
                        .version("v0.0.1"));
    }
}
