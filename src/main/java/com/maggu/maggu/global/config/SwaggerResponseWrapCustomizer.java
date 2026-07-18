package com.maggu.maggu.global.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * 컨트롤러는 DTO를 그대로 반환하지만 실제 응답은
 * {@link com.maggu.maggu.global.response.ResponseWrappingAdvice}가 런타임에 ApiResponse로 감싼다.
 * Swagger 스키마가 실제 응답과 어긋나지 않도록 2xx 응답 스키마를 동일한 형태로 감싼다.
 * 204(No Content)는 어드바이스에서도 감싸지 않으므로 여기서도 제외한다.
 */
@Component
public class SwaggerResponseWrapCustomizer implements OperationCustomizer {

    private static final String NO_CONTENT_STATUS = String.valueOf(HttpStatus.NO_CONTENT.value());

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return operation;
        }

        responses.forEach((statusCode, response) -> {
            if (!statusCode.startsWith("2") || NO_CONTENT_STATUS.equals(statusCode)) {
                return;
            }
            Content content = response.getContent();
            if (content == null) {
                return;
            }
            content.values().forEach(this::wrap);
        });

        return operation;
    }

    private void wrap(MediaType mediaType) {
        Schema<?> original = mediaType.getSchema();
        if (original == null) {
            return;
        }
        mediaType.setSchema(envelope(original));
    }

    private Schema<?> envelope(Schema<?> dataSchema) {
        return new ObjectSchema()
                .addProperty("success", new BooleanSchema().example(true))
                .addProperty("status", new IntegerSchema().example(200))
                .addProperty("code", new StringSchema().example("SUCCESS"))
                .addProperty("message", new StringSchema().example("OK"))
                .addProperty("data", dataSchema)
                .addProperty("timestamp", new DateTimeSchema());
    }
}
