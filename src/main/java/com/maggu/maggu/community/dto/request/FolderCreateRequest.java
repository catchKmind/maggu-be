package com.maggu.maggu.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FolderCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String name;
}