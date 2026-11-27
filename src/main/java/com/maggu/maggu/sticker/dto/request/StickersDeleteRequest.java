package com.maggu.maggu.sticker.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StickersDeleteRequest(
        @NotEmpty(message = "삭제할 항목을 하나 이상 선택해주세요.")
        @Size(max = 20, message = "최대 20개 까지 삭제할 수 있습니다.")
        List<@NotNull Long> stickerIds
) {
}
