package com.maggu.maggu.mypage.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.mypage.dto.request.LocaleUpdateRequest;
import com.maggu.maggu.mypage.dto.response.MyAccountResponse;
import com.maggu.maggu.mypage.dto.response.MyCommentListResponse;
import com.maggu.maggu.mypage.dto.response.MyPostListResponse;
import com.maggu.maggu.mypage.service.MyActivityService;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPage", description = "마이페이지 관련 API")
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageController {
    private static final int MAX_FEED_SIZE = 100;

    private final UserService userService;
    private final MyActivityService myActivityService;

    @Operation(summary = "내 계정 정보 조회", description = "로그인한 본인의 provider/email/nickname/locale을 조회한다.")
    @GetMapping("/account")
    public MyAccountResponse getMyAccount(@CurrentUser AppUser user) {
        return userService.getMyAccount(user); // TODO: 프로필 이미지 응답에 추가
    }

    @Operation(summary = "앱 언어 변경", description = "지도 TourAPI 언어 기본값으로 사용한다. 요청마다 lang을 보내면 그 값이 우선한다.")
    @PatchMapping("/locale")
    public MyAccountResponse updateLocale(
            @CurrentUser AppUser user,
            @Valid @RequestBody LocaleUpdateRequest request
    ) {
        return userService.updateLocale(user, request.locale());
    }

    @Operation(summary = "내가 쓴 글 목록 조회",
            description = "본인이 작성한 게시글을 최신 작성순으로 반환한다. 삭제된 글은 제외하고, 사진 없는 글은 imageUrl이 null이다. "
                    + "탭 상단 표시용 글 수/댓글 수(postCount/commentCount)를 함께 내려준다.")
    @GetMapping("/posts")
    public MyPostListResponse getMyPosts(
            @CurrentUser AppUser user,
            @Parameter(description = "다음 페이지 커서. 첫 요청은 생략하고, 이후에는 직전 응답의 nextCursor를 그대로 넣는다. hasNext가 false면 더 요청하지 않는다")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기. 기본 20, 1~100 범위를 벗어나면 가까운 값으로 보정")
            @RequestParam(defaultValue = "20") int size
    ) {
        int clampSize = Math.max(1, Math.min(size, MAX_FEED_SIZE));

        return myActivityService.getMyPosts(user, cursor, clampSize);
    }

    @Operation(summary = "내가 쓴 댓글 목록 조회",
            description = "본인이 작성한 댓글/답글을 최신 작성순으로 반환한다. 삭제된 댓글과 삭제된 게시글에 단 댓글은 제외한다. "
                    + "탭 상단 표시용 글 수/댓글 수(postCount/commentCount)를 함께 내려준다.")
    @GetMapping("/comments")
    public MyCommentListResponse getMyComments(
            @CurrentUser AppUser user,
            @Parameter(description = "다음 페이지 커서. 첫 요청은 생략하고, 이후에는 직전 응답의 nextCursor를 그대로 넣는다. hasNext가 false면 더 요청하지 않는다")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기. 기본 20, 1~100 범위를 벗어나면 가까운 값으로 보정")
            @RequestParam(defaultValue = "20") int size
    ) {
        int clampSize = Math.max(1, Math.min(size, MAX_FEED_SIZE));

        return myActivityService.getMyComments(user, cursor, clampSize);
    }
}
