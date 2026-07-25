package com.maggu.maggu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TestLoginRequest(
        @NotBlank @Email String email,

        @NotBlank String nickname
) {
}