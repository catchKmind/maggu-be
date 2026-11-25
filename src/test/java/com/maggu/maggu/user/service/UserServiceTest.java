package com.maggu.maggu.user.service;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.storage.CloudFrontUrlResolver;
import com.maggu.maggu.global.storage.PresignedUrlService;
import com.maggu.maggu.global.storage.UploadDomain;
import com.maggu.maggu.user.dto.request.ProfileUpdateRequest;
import com.maggu.maggu.user.dto.response.MyAccountResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final int NICKNAME_GENERATION_MAX_ATTEMPTS = 5;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @Mock
    private CloudFrontUrlResolver cloudFrontUrlResolver;

    @Mock
    private PresignedUrlService presignedUrlService;

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
            assertThat(result.getLocale()).isEqualTo(AppLocale.KO);
        }

        @Test
        @DisplayName("locale을 지정하면 해당 언어로 유저를 생성한다")
        void createUserWithLocale() {
            given(userRepository.existsByNickname("윤시진")).willReturn(false);
            given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

            AppUser result = userService.createUser(
                    Provider.APPLE, "apple-uid", "apple@test.com", "윤시진", AppLocale.EN);

            assertThat(result.getLocale()).isEqualTo(AppLocale.EN);
        }

        @Test
        @DisplayName("preferredNickname이 있고 중복이 아니면 해당 닉네임으로 생성한다")
        void createUserUsesPreferredNicknameWhenAvailable() {
            given(userRepository.existsByNickname("윤시진")).willReturn(false);
            given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

            AppUser result = userService.createUser(Provider.APPLE, "apple-uid", "apple@test.com", "윤시진");

            assertThat(result.getProvider()).isEqualTo(Provider.APPLE);
            assertThat(result.getNickname()).isEqualTo("윤시진");
            verifyNoInteractions(nicknameGenerator);
        }

        @Test
        @DisplayName("preferredNickname이 이미 사용 중이면 자동 생성 닉네임으로 등록한다")
        void createUserFallsBackToGeneratedNicknameWhenPreferredIsTaken() {
            given(userRepository.existsByNickname("윤시진")).willReturn(true);
            given(nicknameGenerator.generate()).willReturn("행복한사자123");
            given(userRepository.existsByNickname("행복한사자123")).willReturn(false);
            given(userRepository.save(any(AppUser.class))).willAnswer(invocation -> invocation.getArgument(0));

            AppUser result = userService.createUser(Provider.APPLE, "apple-uid", "apple@test.com", "윤시진");

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
    @DisplayName("getMyAccount")
    class GetMyAccount {

        @Test
        @DisplayName("전달받은 유저 엔티티의 필드를 그대로 응답 DTO에 담아 반환하고, DB는 조회하지 않는다")
        void getMyAccount() {
            AppUser appUser = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname("행복한사자123")
                    .build();

            MyAccountResponse result = userService.getMyAccount(appUser);

            assertThat(result.getProvider()).isEqualTo(Provider.GOOGLE);
            assertThat(result.getEmail()).isEqualTo("test@test.com");
            assertThat(result.getNickname()).isEqualTo("행복한사자123");
            assertThat(result.getLocale()).isEqualTo(AppLocale.KO);
            assertThat(result.getProfileImageUrl()).isNull();
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("프로필 이미지가 있으면 저장된 objectKey를 CDN URL로 변환해 반환한다")
        void returnsProfileImageAsPublicUrl() {
            AppUser appUser = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname("행복한사자123")
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(appUser, "profileImageUrl", "PROFILE/1/uuid.png");
            given(cloudFrontUrlResolver.toPublicUrl("PROFILE/1/uuid.png"))
                    .willReturn("https://cdn.example.com/PROFILE/1/uuid.png");

            MyAccountResponse result = userService.getMyAccount(appUser);

            assertThat(result.getProfileImageUrl()).isEqualTo("https://cdn.example.com/PROFILE/1/uuid.png");
        }
    }

    @Nested
    @DisplayName("updateLocale")
    class UpdateLocale {

        @Test
        @DisplayName("대상 유저가 없으면 예외를 던진다")
        void throwsWhenUserNotFound() {
            AppUser appUser = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname("행복한사자123")
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(appUser, "id", 1L);
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateLocale(appUser, AppLocale.EN))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }

        @Test
        @DisplayName("locale을 변경하고 계정 응답에 반영한다")
        void updatesLocale() {
            AppUser appUser = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname("행복한사자123")
                    .locale(AppLocale.KO)
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(appUser, "id", 1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(appUser));

            MyAccountResponse result = userService.updateLocale(appUser, AppLocale.EN);

            assertThat(appUser.getLocale()).isEqualTo(AppLocale.EN);
            assertThat(result.getLocale()).isEqualTo(AppLocale.EN);
        }

        @Test
        @DisplayName("프로필 이미지는 인자로 받은 유저가 아니라 DB에서 조회한 유저 기준으로 CDN URL로 변환해 반환한다")
        void returnsProfileImageOfPersistedUser() {
            AppUser principal = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname("행복한사자123")
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(principal, "id", 1L);

            AppUser persisted = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname("행복한사자123")
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(persisted, "id", 1L);
            org.springframework.test.util.ReflectionTestUtils.setField(persisted, "profileImageUrl", "PROFILE/1/uuid.png");

            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));
            given(cloudFrontUrlResolver.toPublicUrl("PROFILE/1/uuid.png"))
                    .willReturn("https://cdn.example.com/PROFILE/1/uuid.png");

            MyAccountResponse result = userService.updateLocale(principal, AppLocale.EN);

            assertThat(result.getProfileImageUrl()).isEqualTo("https://cdn.example.com/PROFILE/1/uuid.png");
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        private static final String OLD_NICKNAME = "행복한사자123";
        private static final String NEW_NICKNAME = "새닉네임";
        private static final String OLD_KEY = "PROFILE/1/11111111-1111-4111-8111-111111111111.png";
        private static final String NEW_KEY = "PROFILE/1/22222222-2222-4222-8222-222222222222.png";

        @Test
        @DisplayName("대상 유저가 없으면 예외를 던지고 검증/반영을 하지 않는다")
        void throwsWhenUserNotFound() {
            AppUser principal = appUser(OLD_NICKNAME, null);
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile(principal, new ProfileUpdateRequest(NEW_NICKNAME, NEW_KEY)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));

            verifyNoInteractions(presignedUrlService);
            verify(userRepository, never()).flush();
        }

        @Test
        @DisplayName("새 닉네임과 새 objectKey를 함께 반영하고, 변경된 계정 정보를 반환한다")
        void updatesNicknameAndProfileImage() {
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));
            given(userRepository.existsByNickname(NEW_NICKNAME)).willReturn(false);
            given(cloudFrontUrlResolver.toPublicUrl(NEW_KEY)).willReturn("https://cdn.example.com/" + NEW_KEY);

            MyAccountResponse result = userService.updateProfile(persisted, new ProfileUpdateRequest(NEW_NICKNAME, NEW_KEY));

            verify(presignedUrlService).validateObjectKey(persisted, UploadDomain.PROFILE, NEW_KEY);
            verify(userRepository).flush();
            assertThat(persisted.getNickname()).isEqualTo(NEW_NICKNAME);
            assertThat(persisted.getProfileImageUrl()).isEqualTo(NEW_KEY);
            assertThat(result.getNickname()).isEqualTo(NEW_NICKNAME);
            assertThat(result.getProfileImageUrl()).isEqualTo("https://cdn.example.com/" + NEW_KEY);
            assertThat(result.getProfileImageKey()).isEqualTo(NEW_KEY);
        }

        @Test
        @DisplayName("닉네임이 현재와 같으면 중복 검사를 하지 않는다")
        void skipsDuplicateCheckForSameNickname() {
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));

            userService.updateProfile(persisted, new ProfileUpdateRequest(OLD_NICKNAME, NEW_KEY));

            verify(userRepository, never()).existsByNickname(any());
            assertThat(persisted.getNickname()).isEqualTo(OLD_NICKNAME);
            assertThat(persisted.getProfileImageUrl()).isEqualTo(NEW_KEY);
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 예외를 던지고 닉네임/이미지 모두 반영하지 않는다")
        void throwsWhenNicknameDuplicate() {
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));
            given(userRepository.existsByNickname(NEW_NICKNAME)).willReturn(true);

            assertThatThrownBy(() -> userService.updateProfile(persisted, new ProfileUpdateRequest(NEW_NICKNAME, NEW_KEY)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_DUPLICATE));

            assertThat(persisted.getNickname()).isEqualTo(OLD_NICKNAME);
            assertThat(persisted.getProfileImageUrl()).isEqualTo(OLD_KEY);
            verifyNoInteractions(presignedUrlService);
            verify(userRepository, never()).flush();
        }

        @Test
        @DisplayName("사전 검사를 통과해도 동시 요청으로 DB 유니크 제약을 위반하면 중복 예외로 변환한다")
        void translatesRaceConditionToDuplicateException() {
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));
            given(userRepository.existsByNickname(NEW_NICKNAME)).willReturn(false);
            willThrow(new DataIntegrityViolationException("uq_user_nickname violation"))
                    .given(userRepository).flush();

            assertThatThrownBy(() -> userService.updateProfile(persisted, new ProfileUpdateRequest(NEW_NICKNAME, OLD_KEY)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_DUPLICATE));
        }

        @Test
        @DisplayName("imageKey가 null이면 objectKey 검증 없이 기본 이미지(null)로 바꾼다")
        void clearsProfileImageWhenImageKeyNull() {
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));

            MyAccountResponse result = userService.updateProfile(persisted, new ProfileUpdateRequest(OLD_NICKNAME, null));

            verifyNoInteractions(presignedUrlService);
            assertThat(persisted.getProfileImageUrl()).isNull();
            assertThat(result.getProfileImageUrl()).isNull();
            assertThat(result.getProfileImageKey()).isNull();
        }

        @Test
        @DisplayName("기존과 같은 objectKey면 검증은 하되 이미지는 그대로 두고 닉네임만 반영한다")
        void keepsProfileImageWhenSameKey() {
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            // JSON 역직렬화처럼 내용은 같지만 다른 String 인스턴스로 요청이 들어오는 상황을 재현
            String sameKeyFromRequest = new String(OLD_KEY);
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));
            given(userRepository.existsByNickname(NEW_NICKNAME)).willReturn(false);
            given(cloudFrontUrlResolver.toPublicUrl(OLD_KEY)).willReturn("https://cdn.example.com/" + OLD_KEY);

            MyAccountResponse result = userService.updateProfile(persisted, new ProfileUpdateRequest(NEW_NICKNAME, sameKeyFromRequest));

            verify(presignedUrlService).validateObjectKey(persisted, UploadDomain.PROFILE, sameKeyFromRequest);
            assertThat(persisted.getNickname()).isEqualTo(NEW_NICKNAME);
            assertThat(persisted.getProfileImageUrl()).isEqualTo(OLD_KEY);
            assertThat(result.getProfileImageUrl()).isEqualTo("https://cdn.example.com/" + OLD_KEY);
            assertThat(result.getProfileImageKey()).isEqualTo(OLD_KEY);
        }

        @Test
        @DisplayName("유효하지 않은 objectKey면 예외를 던지고 닉네임/이미지 모두 반영하지 않는다")
        void throwsWhenObjectKeyInvalid() {
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            String invalidKey = "STICKER/1/22222222-2222-4222-8222-222222222222.png";
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));
            given(userRepository.existsByNickname(NEW_NICKNAME)).willReturn(false);
            willThrow(new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY))
                    .given(presignedUrlService).validateObjectKey(any(), eq(UploadDomain.PROFILE), eq(invalidKey));

            assertThatThrownBy(() -> userService.updateProfile(persisted, new ProfileUpdateRequest(NEW_NICKNAME, invalidKey)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_OBJECT_KEY));

            assertThat(persisted.getNickname()).isEqualTo(OLD_NICKNAME);
            assertThat(persisted.getProfileImageUrl()).isEqualTo(OLD_KEY);
            verify(userRepository, never()).flush();
        }

        @Test
        @DisplayName("인자로 받은 유저가 아니라 DB에서 조회한 유저를 변경하고 그 기준으로 응답한다")
        void updatesPersistedUser() {
            AppUser principal = appUser(OLD_NICKNAME, OLD_KEY);
            AppUser persisted = appUser(OLD_NICKNAME, OLD_KEY);
            persisted.changeLocale(AppLocale.EN);
            given(userRepository.findById(1L)).willReturn(Optional.of(persisted));
            given(userRepository.existsByNickname(NEW_NICKNAME)).willReturn(false);

            MyAccountResponse result = userService.updateProfile(principal, new ProfileUpdateRequest(NEW_NICKNAME, NEW_KEY));

            assertThat(persisted.getNickname()).isEqualTo(NEW_NICKNAME);
            assertThat(principal.getNickname()).isEqualTo(OLD_NICKNAME);
            assertThat(result.getLocale()).isEqualTo(AppLocale.EN);
        }

        private AppUser appUser(String nickname, String profileImageKey) {
            AppUser appUser = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("test@test.com")
                    .nickname(nickname)
                    .locale(AppLocale.KO)
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(appUser, "id", 1L);
            org.springframework.test.util.ReflectionTestUtils.setField(appUser, "profileImageUrl", profileImageKey);
            return appUser;
        }
    }
}
