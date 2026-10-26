package com.ritech.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.ritech.backend.common.ApiException;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** The user id from the token's {@code sub} claim. */
    public static Integer id(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            try {
                return Integer.valueOf(token.getToken().getSubject());
            } catch (NumberFormatException ex) {
                // handled below
            }
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Debes iniciar sesión para continuar.");
    }
}
