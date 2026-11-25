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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final int NICKNAME_GENERATION_MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;
    private final CloudFrontUrlResolver cloudFrontUrlResolver;
    private final PresignedUrlService presignedUrlService;

    @Transactional
    public AppUser createUser(Provider provider, String providerUserId, String email) {
        return createUser(provider, providerUserId, email, null);
    }

    @Transactional
    public AppUser createUser(Provider provider, String providerUserId, String email, String preferredNickname) {
        return createUser(provider, providerUserId, email, preferredNickname, AppLocale.KO);
    }

    @Transactional
    public AppUser createUser(Provider provider, String providerUserId, String email, String preferredNickname,
                              AppLocale locale) {
        AppUser appUser = AppUser.builder()
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .nickname(resolveNickname(preferredNickname))
                .locale(locale == null ? AppLocale.KO : locale)
                .build();
        return userRepository.save(appUser);
    }

    @Transactional
    public MyAccountResponse updateLocale(AppUser user, AppLocale locale) {
        AppUser appUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        String profileImageUrl = cloudFrontUrlResolver.toPublicUrl(appUser.getProfileImageUrl());
        appUser.changeLocale(locale);
        return MyAccountResponse.from(appUser, profileImageUrl);
    }

    @Transactional(readOnly = true)
    public MyAccountResponse getMyAccount(AppUser user) {
        String profileImageUrl = cloudFrontUrlResolver.toPublicUrl(user.getProfileImageUrl());
        return MyAccountResponse.from(user, profileImageUrl);
    }

    @Transactional
    public MyAccountResponse updateProfile(AppUser user, ProfileUpdateRequest request) {
        AppUser appUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // 닉네임 검증
        if (!Objects.equals(appUser.getNickname(), request.nickname())
                && userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }

        // 이미지 검증
        if (request.imageKey() != null) {
            presignedUrlService.validateObjectKey(user, UploadDomain.PROFILE, request.imageKey());
        }

        // 검증 후 반영
        appUser.changeNickname(request.nickname());

        if (request.imageKey() == null) {
            appUser.clearProfileImageUrl();
        } else if (!Objects.equals(appUser.getProfileImageUrl(), request.imageKey())) {
            appUser.updateProfileImageUrl(request.imageKey());
        }

        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }

        return MyAccountResponse.from(appUser, cloudFrontUrlResolver.toPublicUrl(appUser.getProfileImageUrl()));
    }

    // ---------- private methods ----------
    private String resolveNickname(String preferredNickname) {
        if (preferredNickname != null && !preferredNickname.isBlank()) {
            String candidate = clipNickname(preferredNickname.trim());
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        return generateUniqueNickname();
    }

    private String clipNickname(String nickname) {
        return nickname.length() <= 30 ? nickname : nickname.substring(0, 30);
    }

    private String generateUniqueNickname() {
        for (int attempt = 0; attempt < NICKNAME_GENERATION_MAX_ATTEMPTS; attempt++) {
            String candidate = nicknameGenerator.generate();
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "닉네임 생성 재시도 초과");
    }
}
