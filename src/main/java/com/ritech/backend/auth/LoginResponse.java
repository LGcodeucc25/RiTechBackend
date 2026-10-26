package com.ritech.backend.auth;

import java.time.OffsetDateTime;

public record LoginResponse(String token, OffsetDateTime expiresAt, UserDto user) {
}
