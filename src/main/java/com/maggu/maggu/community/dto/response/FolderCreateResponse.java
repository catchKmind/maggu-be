package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FolderCreateResponse {
    private Long folderId;
    private String name;
}