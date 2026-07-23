package com.maggu.maggu.user.service;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.mypage.dto.MyAccountResponse;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final int NICKNAME_GENERATION_MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    @Transactional
    public AppUser createUser(Provider provider, String providerUserId, String email) {
        AppUser appUser = AppUser.builder()
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .nickname(generateUniqueNickname())
                .build();
        return userRepository.save(appUser);
    }

    @Transactional
    public void updateNickname(Long userId, String newNickname) {
        AppUser appUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!appUser.getNickname().equals(newNickname) && userRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }

        appUser.changeNickname(newNickname);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    @Transactional(readOnly = true)
    public MyAccountResponse getMyAccount(AppUser user) {
        return MyAccountResponse.from(user);
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
