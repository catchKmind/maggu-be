package com.maggu.maggu.sticker.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.sticker.dto.StickerCreateRequest;
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

    @Operation(summary = "내 스티커 목록 조회", description = "커스텀 스티커 목록을 조회한다.")
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
}
