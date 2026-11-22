package com.maggu.maggu.community.dto.request;

import com.maggu.maggu.community.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCreateRequest {

    @Schema(description = "신고할 게시글 ID. 댓글 신고면 생략", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long postId;

    @Schema(description = "신고할 댓글 ID. 게시글 신고면 생략", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long commentId;

    @NotNull
    private ReportReason reason;
}