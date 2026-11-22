package com.maggu.maggu.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateRequest {

    @NotBlank
    @Size(max = 120)
    @Schema(description = "댓글 본문", example = "여기 주차 되나요?")
    private String content;

    @Schema(description = "원댓글 또는 대댓글 ID. 원댓글 작성 시 생략. 대댓글 ID를 보내도 원댓글 아래로 붙는다",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long parentCommentId;
}
