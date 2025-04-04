package com.kimdevspace.musicexplorer.spotify.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class SpotifyServiceIntegrationTest {

    @Autowired
    private SpotifyAuthService authService;

    @Test
    void testAuthTokenGeneration() {
        // Given, When
        boolean isTokenValid = authService.isTokenValid();

        // Then
        System.out.println("토근 유효 여부: " + isTokenValid);
        if (isTokenValid) {
            System.out.println("현재 토큰이 유효합니다.");
        } else {
            authService.refreshAccessToken();
            System.out.println("토큰을 갱신했습니다.");
            assertTrue(authService.isTokenValid());
        }
    }
}
