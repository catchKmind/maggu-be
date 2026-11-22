package com.maggu.maggu.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FolderCreateRequest {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "폴더명", example = "가고 싶은 곳")
    private String name;
}
