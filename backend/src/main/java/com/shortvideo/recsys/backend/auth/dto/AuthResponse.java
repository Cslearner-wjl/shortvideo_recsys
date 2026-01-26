package com.shortvideo.recsys.backend.auth.dto;

public record AuthResponse(String token, AuthUserDto user) {
}

