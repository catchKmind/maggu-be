package com.maggu.maggu.sticker.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.sticker.dto.request.GiphyStickerCreateRequest;
import com.maggu.maggu.sticker.dto.request.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.request.StickersDeleteRequest;
import com.maggu.maggu.sticker.dto.response.StickerDeleteResponse;
import com.maggu.maggu.sticker.dto.response.StickerResponse;
import com.maggu.maggu.sticker.dto.response.StickersDeleteResponse;
import com.maggu.maggu.sticker.service.StickerService;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "내 스티커 생성",
            description = "POST /api/v1/uploads/presigned-url(domain=STICKER)로 발급받은 objectKey를 imageUrl 필드에 전달한다.")
    @PostMapping
    public StickerResponse createMySticker(@CurrentUser AppUser user,
                                           @Valid @RequestBody StickerCreateRequest request) {

        return stickerService.createMySticker(user, request);
    }

    @Operation(summary = "내 스티커 삭제", description = "본인이 생성한 스티커만 삭제할 수 있다.")
    @DeleteMapping("/{stickerId}")
    public StickerDeleteResponse deleteMySticker(@CurrentUser AppUser user,
                                                 @Parameter(description = "스티커 ID", example = "1") @PathVariable Long stickerId) {
        return stickerService.deleteMySticker(user, stickerId);
    }

    @Operation(summary = "내 스티커 다중 삭제",
            description = "본인이 생성한 스티커만 삭제할 수 있다. 한 번에 최대 20개까지 요청 가능 \n\n" +
                    "- 다른 유저의 스티커가 하나라도 포함되면 403(STICKER-002)을 반환하고, 아무것도 삭제하지 않는다.\n" +
                    "- 존재하지 않거나 이미 삭제된 ID는 무시하고 성공으로 처리한다. 응답의 stickerIds에는 요청한 ID가 모두 포함된다.\n" +
                    "- 게시글 반응이나 장소 스크랩에 사용 중인 스티커는 내 스티커 목록에서만 사라지고, " +
                    "이미 붙은 반응/장소 스크랩에는 그대로 남는다.")
    @PostMapping("/bulk-delete")
    public StickersDeleteResponse deleteMyStickers(@CurrentUser AppUser user,
                                                   @Valid @RequestBody StickersDeleteRequest request) {
        return stickerService.deleteMyStickers(user, request);
    }
}
