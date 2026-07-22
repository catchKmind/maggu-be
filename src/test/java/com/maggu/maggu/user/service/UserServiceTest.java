package com.maggu.maggu.user.service;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final int NICKNAME_GENERATION_MAX_ATTEMPTS = 5;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("생성된 닉네임이 중복되지 않으면 그대로 사용해 유저를 생성한다")
        void createUser() {
            given(nicknameGenerator.generate()).willReturn("행복한사자123");
            given(userRepository.existsByNickname("행복한사자123")).willReturn(false);
            given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

            AppUser result = userService.createUser(Provider.GOOGLE, "google-uid", "test@test.com");

            assertThat(result.getProvider()).isEqualTo(Provider.GOOGLE);
            assertThat(result.getProviderUserId()).isEqualTo("google-uid");
            assertThat(result.getEmail()).isEqualTo("test@test.com");
            assertThat(result.getNickname()).isEqualTo("행복한사자123");
        }

        @Test
        @DisplayName("생성된 닉네임이 중복되면 재시도해서 중복되지 않는 닉네임으로 생성한다")
        void createUserRetriesOnNicknameCollision() {
            given(nicknameGenerator.generate()).willReturn("중복사자1", "새로운사자2");
            given(userRepository.existsByNickname("중복사자1")).willReturn(true);
            given(userRepository.existsByNickname("새로운사자2")).willReturn(false);
            given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

            AppUser result = userService.createUser(Provider.GOOGLE, "google-uid", "test@test.com");

            assertThat(result.getNickname()).isEqualTo("새로운사자2");
        }

        @Test
        @DisplayName("재시도 횟수를 모두 소진하면 예외를 던지고 유저를 생성하지 않는다")
        void createUserThrowsWhenRetriesExhausted() {
            given(nicknameGenerator.generate()).willReturn("항상중복인닉네임");
            given(userRepository.existsByNickname("항상중복인닉네임")).willReturn(true);

            assertThatThrownBy(() -> userService.createUser(Provider.GOOGLE, "google-uid", "test@test.com"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));

            verify(nicknameGenerator, times(NICKNAME_GENERATION_MAX_ATTEMPTS)).generate();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateNickname")
    class UpdateNickname {

        @Test
        @DisplayName("대상 유저가 없으면 예외를 던진다")
        void updateNicknameThrowsWhenUserNotFound() {
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateNickname(1L, "새닉네임"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 예외를 던지고 반영하지 않는다")
        void updateNicknameThrowsWhenNicknameDuplicate() {
            AppUser appUser = appUserWithNickname("기존닉네임");
            given(userRepository.findById(1L)).willReturn(Optional.of(appUser));
            given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

            assertThatThrownBy(() -> userService.updateNickname(1L, "중복닉네임"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_DUPLICATE));

            assertThat(appUser.getNickname()).isEqualTo("기존닉네임");
            verify(userRepository, never()).flush();
        }

        @Test
        @DisplayName("현재와 동일한 닉네임으로 변경 요청하면 중복 체크 없이 통과한다")
        void updateNicknameSkipsDuplicateCheckForSameNickname() {
            AppUser appUser = appUserWithNickname("동일닉네임");
            given(userRepository.findById(1L)).willReturn(Optional.of(appUser));

            userService.updateNickname(1L, "동일닉네임");

            verify(userRepository, never()).existsByNickname(any());
            verify(userRepository).flush();
        }

        @Test
        @DisplayName("중복되지 않은 새 닉네임으로 정상 변경한다")
        void updateNickname() {
            AppUser appUser = appUserWithNickname("기존닉네임");
            given(userRepository.findById(1L)).willReturn(Optional.of(appUser));
            given(userRepository.existsByNickname("새닉네임")).willReturn(false);

            userService.updateNickname(1L, "새닉네임");

            assertThat(appUser.getNickname()).isEqualTo("새닉네임");
            verify(userRepository).flush();
        }

        @Test
        @DisplayName("사전 체크를 통과해도 동시 요청으로 DB 유니크 제약을 위반하면 중복 예외로 변환한다")
        void updateNicknameTranslatesRaceConditionToDuplicateException() {
            AppUser appUser = appUserWithNickname("기존닉네임");
            given(userRepository.findById(1L)).willReturn(Optional.of(appUser));
            given(userRepository.existsByNickname("새닉네임")).willReturn(false);
            willThrow(new DataIntegrityViolationException("uq_user_nickname violation"))
                    .given(userRepository).flush();

            assertThatThrownBy(() -> userService.updateNickname(1L, "새닉네임"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_DUPLICATE));
        }

        private AppUser appUserWithNickname(String nickname) {
            return AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname(nickname)
                    .build();
        }
    }
}
