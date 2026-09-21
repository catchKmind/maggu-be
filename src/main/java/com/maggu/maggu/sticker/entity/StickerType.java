package com.maggu.maggu.sticker.entity;

import io.swagger.v3.oas.annotations.media.Schema;

public enum StickerType {
    @Schema(description = "운영자 관리 마스터 스티커 (user_id null)")
    MASTER,

    @Schema(description = "유저가 사진을 찍어 누끼 딴 개인 커스텀 스티커 (user_id = 소유자)")
    CUSTOM,

    @Schema(description = "GIPHY 연동 스티커")
    GIPHY
}
