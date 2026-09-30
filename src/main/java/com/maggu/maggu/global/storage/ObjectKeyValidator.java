package com.maggu.maggu.global.storage;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ObjectKeyValidator {

    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile(
            "^(STICKER|POST)/(\\d+)/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpeg|jpg|png|heic|webp)$",
            Pattern.CASE_INSENSITIVE
    );

    public String validateStickerObjectKey(AppUser user, String raw) {
        return validate(UploadDomain.STICKER, user.getId(), raw);
    }

    public List<String> validatePostObjectKeys(AppUser user, List<String> rawKeys) {
        if (rawKeys == null || rawKeys.isEmpty()) {
            return List.of();
        }
        for (String raw : rawKeys) {
            if (!StringUtils.hasText(raw)) {
                throw new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY);
            }
        }
        return rawKeys.stream()
                .map(String::trim)
                .map(key -> validate(UploadDomain.POST, user.getId(), key))
                .toList();
    }

    private String validate(UploadDomain expectedDomain, Long userId, String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY);
        }
        String normalized = trimLeadingSlash(raw.trim());
        if (normalized.contains("://") || normalized.contains("?")) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY);
        }

        Matcher matcher = OBJECT_KEY_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY);
        }

        UploadDomain domain = UploadDomain.from(matcher.group(1));
        if (domain != expectedDomain) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY);
        }

        long keyUserId = Long.parseLong(matcher.group(2));
        if (!userId.equals(keyUserId)) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY);
        }

        return normalized;
    }

    private static String trimLeadingSlash(String value) {
        int index = 0;
        while (index < value.length() && value.charAt(index) == '/') {
            index++;
        }
        return value.substring(index);
    }
}
