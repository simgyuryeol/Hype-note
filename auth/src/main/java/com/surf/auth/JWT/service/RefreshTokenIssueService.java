package com.surf.auth.JWT.service;

import com.surf.auth.JWT.provider.TokenProvider;
import com.surf.auth.auth.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class RefreshTokenIssueService {

    private final TokenProvider tokenProvider;
    private final RedisRefreshTokenSaveService redisRefreshTokenSaveService;

    @Value("${jwt.refresh-token-expiration-time}")
    private long EXPIRATION_TIME;

    @Value("${jwt.refresh-token-cookie-name}")
    private String REFRESH_TOKEN_COOKIE_NAME;

    @Value("${jwt.refresh-token-cookie-path}")
    private String REFRESH_TOKEN_COOKIE_PATH;

    @Value("${jwt.refresh-token-cookie-max-age}")
    private int REFRESH_TOKEN_COOKIE_MAX_AGE;

    public void refreshTokenIssue(User userInfo, HttpServletResponse response) {
        Map<String, Object> claims = new HashMap<>();
        String refreshToken = tokenProvider.createToken(claims, userInfo, EXPIRATION_TIME);

        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        refreshTokenCookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);
        refreshTokenCookie.setPath(REFRESH_TOKEN_COOKIE_PATH);
        refreshTokenCookie.setHttpOnly(true);
        response.addCookie(refreshTokenCookie);

        redisRefreshTokenSaveService.saveRefreshToken(EXPIRATION_TIME, refreshToken, userInfo);

    }
}
