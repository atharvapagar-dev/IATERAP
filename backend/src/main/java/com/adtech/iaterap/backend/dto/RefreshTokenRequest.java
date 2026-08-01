package com.adtech.iaterap.backend.dto;

/**
 * Request body shared by /api/auth/refresh (rotate) and /api/auth/logout
 * (revoke), which both only ever need the presented refresh token. AI-156.
 */
public class RefreshTokenRequest {

    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
