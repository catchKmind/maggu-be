package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FolderResponse {
    private Long folderId;
    private String name;
    private boolean isDefault;
}