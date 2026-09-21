package com.maggu.maggu.sticker.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.sticker.dto.GiphyStickerCreateRequest;
import com.maggu.maggu.sticker.dto.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.StickerDeleteResponse;
import com.maggu.maggu.sticker.dto.StickerResponse;
import com.maggu.maggu.sticker.service.StickerService;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sticker", description = "스티커 관련 API")
@RestController
@RequestMapping("/api/v1/stickers")
@RequiredArgsConstructor
public class StickerController {

    private final StickerService stickerService;

    @Operation(
            summary = "GIPHY 스티커 생성",
            description = "GIPHY 공식 API 정책상 BE는 GIPHY API를 호출하지 않는다. " +
                    "  FE가 GIPHY를 직접 호출해서 처리해야 하고, 이 API는 FE가 이미 고른 GIF의 giphyId와 실제 이미지 URL만 넘겨받아 저장하는 역할만 한다."
    )

    @PostMapping("/giphy")
    public StickerResponse createGiphySticker(@Valid @RequestBody GiphyStickerCreateRequest request) {
        return stickerService.createGiphySticker(request);
    }

    @Operation(summary = "내 스티커 목록 조회")
    @GetMapping
    public List<StickerResponse> getMyStickers(@CurrentUser AppUser user) {

        return stickerService.getMyStickers(user);
    }

    @Operation(summary = "내 스티커 생성")
    @PostMapping
    public StickerResponse createMySticker(@CurrentUser AppUser user,
                                           @Valid @RequestBody StickerCreateRequest request) {

        return stickerService.createMySticker(user, request);
    }

    @Operation(summary = "내 스티커 삭제", description = "본인이 생성한 스티커만 삭제할 수 있다.")
    @DeleteMapping("/{stickerId}")
    public StickerDeleteResponse deleteMySticker(@CurrentUser AppUser user,
                                                 @PathVariable Long stickerId) {
        return stickerService.deleteMySticker(user, stickerId);
    }
}
