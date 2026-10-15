package com.maggu.maggu.place.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.place.entity.PlaceScrap;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record PlaceScrapCursor(
        Instant createdAt, Long id
) {
    public static PlaceScrapCursor from(PlaceScrap placeScrap) {
        return new PlaceScrapCursor(placeScrap.getCreatedAt(), placeScrap.getId());
    }

    public String encode() {
        return Base64.getEncoder()
                .encodeToString(String.format("%d:%d", createdAt.toEpochMilli(), id).getBytes(StandardCharsets.UTF_8));
    }

    public static PlaceScrapCursor decode(String cursor) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String decodedText = new String(decodedBytes, StandardCharsets.UTF_8);

            return new PlaceScrapCursor(Instant.ofEpochMilli(Long.parseLong(decodedText.split(":")[0])),
                    Long.parseLong(decodedText.split(":")[1]));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
