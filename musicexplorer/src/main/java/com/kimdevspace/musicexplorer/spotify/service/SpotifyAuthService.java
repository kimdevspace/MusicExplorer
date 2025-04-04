package com.kimdevspace.musicexplorer.spotify.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyAuthService {

    private final SpotifyApi spotifyApi;
    private Instant tokenExpirationTime;

    @Value("${spotify.auth.token-renewal-threshold-ms:600000}")
    private long tokenRenewalThresholdMs;

    @PostConstruct
    public void initialize() {
        refreshAccessToken();
    }

    public void refreshAccessToken() {
        try {
            ClientCredentialsRequest request = spotifyApi.clientCredentials().build();
            ClientCredentials credentials = request.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());

            // 토큰 만료 시간 설정 (현재 시간 + 만료 시간(초))
            tokenExpirationTime = Instant.now().plusSeconds(credentials.getExpiresIn());

            log.info("Spotify API token refreshed, expires at: {}",
                    LocalDateTime.ofInstant(tokenExpirationTime, ZoneId.systemDefault()));

        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            log.error("Error refreshing Spotify API token", e);
            throw new RuntimeException("Failed to authenticate with Spotify API", e);
        }
    }

    @Scheduled(fixedRate = 1800000) // 30분마다 토큰 상태 확인
    public void checkAndRefreshToken() {
        if (isTokenExpiringSoon()) {
            log.info("Spotify token is expiring soon. Refreshing...");
            refreshAccessToken();
        }
    }

    private boolean isTokenExpiringSoon() {
        if (tokenExpirationTime == null) {
            return true;
        }

        // 토큰 만료까지 설정된 임계값보다 적게 남았는지 확인
        return Instant.now().plusMillis(tokenRenewalThresholdMs).isAfter(tokenExpirationTime);
    }

    // 현재 액세스 토큰이 유효한지 확인
    public boolean isTokenValid() {
        return spotifyApi.getAccessToken() != null &&
                tokenExpirationTime != null &&
                Instant.now().isBefore(tokenExpirationTime);
    }
}