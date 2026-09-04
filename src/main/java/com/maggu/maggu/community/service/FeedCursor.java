package com.maggu.maggu.community.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.entity.Post;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record FeedCursor(Integer scrapCount, Instant createdAt, Long id) {
    public static FeedCursor from(Post post) {
        return new FeedCursor(post.getScrapCount(), post.getCreatedAt(), post.getId());
    }

    public String encode() {
        return Base64.getEncoder()
                .encodeToString(String.format("%d:%d:%d", scrapCount, createdAt.toEpochMilli(), id).getBytes(StandardCharsets.UTF_8));
    }

    public static FeedCursor decode(String cursor) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String decodedText = new String(decodedBytes, StandardCharsets.UTF_8);

            return new FeedCursor(Integer.parseInt(decodedText.split(":")[0]),
                    Instant.ofEpochMilli(Long.parseLong(decodedText.split(":")[1])),
                    Long.parseLong(decodedText.split(":")[2]));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
