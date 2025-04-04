package com.kimdevspace.musicexplorer.spotify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpotifyAuthServiceTest {

    @Mock
    private SpotifyApi spotifyApi;

    @Mock
    private ClientCredentialsRequest clientCredentialsRequest;

    @InjectMocks
    private SpotifyAuthService spotifyAuthService;

    @BeforeEach
    void setUp() {
        // 설정 값 주입
        ReflectionTestUtils.setField(spotifyAuthService, "tokenRenewalThresholdMs", 600000L);
    }

    /**
     * Spotify API 클라이언트 자격 증명 요청을 실행하는지
     * 응답에서 액세스 토큰을 SpotifyApi 객체에 설정하는지
     * 토큰 만료 시간을 현재 시간 이후로 올바르게 계산하는지
     */
    @Test
    void refreshAccessToken_ShouldSetTokenAndExpirationTime() throws Exception {
        // Given
        ClientCredentials clientCredentials = mock(ClientCredentials.class);
        when(clientCredentials.getAccessToken()).thenReturn("test-access-token");
        when(clientCredentials.getExpiresIn()).thenReturn(3600);

        ClientCredentialsRequest.Builder builder = mock(ClientCredentialsRequest.Builder.class);
        when(spotifyApi.clientCredentials()).thenReturn(builder);

        when(builder.build()).thenReturn(clientCredentialsRequest);
        when(clientCredentialsRequest.execute()).thenReturn(clientCredentials);

        // When
        spotifyAuthService.refreshAccessToken();

        // Then
        verify(spotifyApi).setAccessToken("test-access-token");
        verify(clientCredentialsRequest).execute();

        // tokenExpirationTime 필드는 private이므로 리플렉션을 통해 검증
        Instant tokenExpirationTime = (Instant) ReflectionTestUtils.getField(spotifyAuthService, "tokenExpirationTime");
        assertNotNull(tokenExpirationTime);
        assertTrue(tokenExpirationTime.isAfter(Instant.now()));
    }

    /**
     * 토큰이 존재하고 만료 시간이 미래인 경우 isTokenValid()가 true를 반환하는지 확인
     */
    @Test
    void isTokenValid_WithValidToken_ReturnsTrue() {
        // Given
        when(spotifyApi.getAccessToken()).thenReturn("valid-token");

        // 만료 시간을 현재로부터 1시간 후로 설정
        Instant futureTime = Instant.now().plusSeconds(3600);
        ReflectionTestUtils.setField(spotifyAuthService, "tokenExpirationTime", futureTime);

        // When
        boolean isValid = spotifyAuthService.isTokenValid();

        // Then
        assertTrue(isValid);
    }

    /**
     * 토큰이 곧 만료될 예정인 경우 (5분), checkAndRefreshToken()이 refreshAccessToken()을 호출하는지 확인
     */
    @Test
    void checkAndRefreshToken_ShouldRefreshWhenTokenExpiringSoon() throws Exception {
        // Given
        // 토큰이 곧 만료되도록 설정한다. (현재시간 + 5분)
        Instant soonExpiringTime = Instant.now().plusSeconds(300);
        ReflectionTestUtils.setField(spotifyAuthService, "tokenExpirationTime", soonExpiringTime);

        // 토큰 갱신에 필요한 모킹 설정
        ClientCredentials clientCredentials = mock(ClientCredentials.class);
        when(clientCredentials.getAccessToken()).thenReturn("new-test-access-token");
        when(clientCredentials.getExpiresIn()).thenReturn(3600);

        ClientCredentialsRequest.Builder builder = mock(ClientCredentialsRequest.Builder.class);
        when(spotifyApi.clientCredentials()).thenReturn(builder);
        when(builder.build()).thenReturn(clientCredentialsRequest);
        when(clientCredentialsRequest.execute()).thenReturn(clientCredentials);

        // SpotifyAuthService를 부분 모킹하여 refreshAccessToken 메서드 호출 확인
        SpotifyAuthService spyService = spy(spotifyAuthService);

        // When
        spyService.checkAndRefreshToken();

        // Then
        verify(spyService).refreshAccessToken();
        verify(spotifyApi).setAccessToken("new-test-access-token");
    }

    /**
     * 토큰이 충분히 유혀한 경우 checkAndRefreshToken()이 refreshAccessToken()을 호출하지 않는지 확인
     */
    @Test
    void checkAndRefreshToken_ShouldNotRefreshWhenTokenValid() {
        // Given
        // 토큰이 아직 유효하도록 설정 (현재 시간 + 2시간)
        Instant validTime = Instant.now().plusSeconds(7200);
        ReflectionTestUtils.setField(spotifyAuthService, "tokenExpirationTime", validTime);

        // SpotifyAuthService를 부분 모킹하여 refreshAccessToken 메서드 호출 확인
        SpotifyAuthService spyService = spy(spotifyAuthService);

        // When
        spyService.checkAndRefreshToken();

        // Then
        verify(spyService, never()).refreshAccessToken();
    }

    /**
     * initialize() 메서드가 refreshAccessToken()을 호출하는지 확인
     */
    @Test
    void initialize_ShouldCallRefreshAccessToken() {
        // Given
        SpotifyAuthService spyService = spy(spotifyAuthService);
        doNothing().when(spyService).refreshAccessToken();

        // When
        spyService.initialize();

        // Then
        verify(spyService).refreshAccessToken();
    }
}
