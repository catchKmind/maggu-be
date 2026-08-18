package com.maggu.maggu.community.dto.request;

import com.maggu.maggu.community.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCreateRequest {

    private Long postId;

    private Long commentId;

    @NotNull
    private ReportReason reason;
}