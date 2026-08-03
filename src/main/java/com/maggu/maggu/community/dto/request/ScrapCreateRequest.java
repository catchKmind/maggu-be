package com.maggu.maggu.community.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScrapCreateRequest {

    @NotNull
    private Long postId;

    // null이면 기본 폴더로 저장
    private Long folderId;
}