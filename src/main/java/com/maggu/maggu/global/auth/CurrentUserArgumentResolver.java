package com.maggu.maggu.global.auth;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import com.maggu.maggu.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    // TODO: 인증 도입 전까지만 사용하는 임시 유저. 실제 로그인 붙으면 이 클래스 내부만 교체
    private static final String TEST_PROVIDER_USER_ID = "dev-test-user";

    private final UserRepository userRepository;
    private final UserService userService;

    // 1. 어떤 파라미터에 이 리졸버를 적용할지 결정
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentUser가 붙어 있고, 타입까지 User인 경우에만 처리
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AppUser.class.isAssignableFrom(parameter.getParameterType());
    }

    // 2. supportsParameter가 true를 반환하면, 실제로 파라미터에 주입할 객체를 생성
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        // [지금 - 임시 구현 단계] DB에 저장된 실제 유저를 find-or-create로 반환 (id가 없는 객체를 주면 안 됨)
        return userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, TEST_PROVIDER_USER_ID)
                .orElseGet(() -> userService.createUser(Provider.GOOGLE, TEST_PROVIDER_USER_ID, "test@gmail.com"));

        /*
        [나중에 - 보안/시큐리티 도입 단계]
        컨트롤러 코드는 건드릴 필요 없이, 여기만 아래처럼 바꾸면 됩니다.

        String token = webRequest.getHeader("Authorization");
        Long userId = jwtProvider.getUserId(token);
        return userRepository.findById(userId);
        */
    }
}
