package com.maggu.maggu.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 컨트롤러가 원래 반환하려던 DTO를 그대로 리턴하면 이 어드바이스가 {@link ApiResponse}로 감싼다.
 * String 리턴 타입은 StringHttpMessageConverter와의 알려진 캐스팅 이슈 때문에 감싸지 않으니,
 * 컨트롤러는 순수 String을 직접 반환하지 말 것.
 */
@RestControllerAdvice(basePackages = "com.maggu.maggu")
public class ResponseWrappingAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();
        return !ApiResponse.class.isAssignableFrom(type) && !String.class.isAssignableFrom(type);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        int status = (response instanceof ServletServerHttpResponse servletResponse)
                ? servletResponse.getServletResponse().getStatus()
                : HttpStatus.OK.value();

        if (status == HttpStatus.NO_CONTENT.value()) {
            return body;
        }

        return ApiResponse.success(status, body);
    }
}
