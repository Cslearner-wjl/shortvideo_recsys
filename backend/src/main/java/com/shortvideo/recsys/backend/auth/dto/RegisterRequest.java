package com.shortvideo.recsys.backend.auth.dto;

public record RegisterRequest(
        String username,
        String phone,
        String email,
        String password,
        String emailCode
) {
}

