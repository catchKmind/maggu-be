package com.maggu.maggu.sticker.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.sticker.dto.StickerResponse;
import com.maggu.maggu.sticker.service.StickerService;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

/*이번 PR 스코프 (지금 구현한 것)
  ## 제목
feat: sticker 패키지 분리 및 커스텀 스티커 목록 조회 API 추가

  ## 요약
  - Sticker/StickerRepository를 별도 sticker 패키지로 분리
  - Sticker에 소유자(user_id, nullable) 필드 추가 — 운영자 스티커(NULL)와 유저 커스텀 스티커를 같은 테이블에서 구분
  - GET /api/v1/stickers — 로그인 유저 본인의 커스텀 스티커 목록 조회 API 추가

  ## 주요 변경 사항
  **패키지 분리**
        - Sticker, StickerRepository → com.maggu.maggu.sticker로 이전
  - PostStickerReaction 등 반응 관련 로직은 community에 유지 (스티커 "정의"와 "게시물 반응 사용"을 분리)

  **소유권 모델**
        - Sticker.user_id는 nullable — NULL이면 운영자 관리 스티커, 값이 있으면 커스텀 스티커
  - Giphy 스티커는 DB에 저장하지 않기로 결정(별도 이슈에서 실시간 프록시로 구현 예정)

  **API**
        - GET /api/v1/stickers — @CurrentUser로 인증된 유저의 커스텀 스티커만 반환 (findAllByUser)

  ## ⚠️ 참고 (리뷰어 확인 필요)
  - 회원 탈퇴 시 sticker.user_id FK 처리(@OnDelete) 미정 — 커스텀 스티커가 이미 post_sticker_reaction에서 참조 중인 상태로 소유자가 탈퇴하면 어떻게 처리할지 별도 확인 필요
        (프로젝트 전체적으로 @OnDelete 애너테이션 도입이 트래킹 중인 이슈라 이번 건도 그 트랙에 포함)
  - 커스텀 스티커 업로드(S3), Giphy 실시간 연동은 이번 PR 범위 밖 — 후속 이슈로 분리

후속 이슈로 분리할 것
  1. feat: 커스텀 스티커 업로드 API — 카메라 촬영 이미지 S3 업로드 + Sticker row 생성
  2. feat: Giphy 스티커 실시간 프록시 연동 — GiphyClient(RestClient) 추가, GET /api/v1/stickers 응답에 Giphy 섹션 병합

올리시겠어요?
*/