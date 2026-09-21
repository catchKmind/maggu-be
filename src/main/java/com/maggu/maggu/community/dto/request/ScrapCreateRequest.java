package com.maggu.maggu.community.dto.request;

import jakarta.validation.constraints.NotNull;

public record ScrapCreateRequest(
        @NotNull Long postId,
        Long folderId
) {
}
