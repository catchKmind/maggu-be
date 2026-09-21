package com.maggu.maggu.community.dto.response;

import lombok.Builder;

@Builder
public record FolderResponse(
        Long folderId,
        String name,
        boolean isDefault
) {
}
