package com.kimdevspace.musicexplorer.spotify.service;

import com.kimdevspace.musicexplorer.domain.entity.Track;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumsTracksRequest;
import se.michaelthelin.spotify.requests.data.browse.GetListOfNewReleasesRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final SpotifyApi spotifyApi;
    private final SpotifyAuthService authService;

    /**
     * Spotify API에서 인기 트랙을 가져옵니다.
     * (실제로는 Spotify API가 곧바로 인기 트랙을 제공하지 않아서
     * 플레이리스트나 특정 차트를 사용해야 합니다)
     */
    @Retryable(
            value = {IOException.class, SpotifyWebApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public List<Track> getPopularTracks(String playlistId, int limit) {
        ensureValidToken();

        try {
            // 여기서는 특정 플레이리스트의 트랙을 가져오는 것으로 대체합니다
            // 실제 구현에서는 인기 플레이리스트 ID를 사용해야 합니다
            return Arrays.stream(
                    spotifyApi.getPlaylistsItems(playlistId)
                            .limit(limit)
                            .build()
                            .execute()
                            .getItems())
                    .map(playlistTrack -> {
                        Track track = (Track) playlistTrack.getTrack();
                        return track;
                    })
                    .toList();
        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            log.error("Error fetching popular tracks from Spotify", e);
            return Collections.emptyList();
        }
    }

    /**
     * Spotify API에서 신규 발매 앨범을 가져옵니다.
     */
    @Retryable(
            value = {IOException.class, SpotifyWebApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public List<AlbumSimplified> getNewReleases(int limit) {
        ensureValidToken();

        try {
            GetListOfNewReleasesRequest request = spotifyApi.getListOfNewReleases()
                    .limit(limit)
                    .build();

            Paging<AlbumSimplified> albums = request.execute();
            return Arrays.asList(albums.getItems());
        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            log.error("Error fetching new releases from Spotify", e);
            return Collections.emptyList();
        }
    }

    /**
     * 앨범의 트랙 목록을 가져옵니다.
     */
    @Retryable(
            value = {IOException.class, SpotifyWebApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public List<TrackSimplified> getAlbumTracks(String albumId) {
        ensureValidToken();

        try {
            GetAlbumsTracksRequest request = spotifyApi.getAlbumsTracks(albumId)
                    .build();

            Paging<TrackSimplified> tracks = request.execute();
            return Arrays.asList(tracks.getItems());
        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            log.error("Error fetching album tracks from Spotify: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 특정 트랙의 상세 정보를 가져옵니다.
     */
    @Retryable(
            value = {IOException.class, SpotifyWebApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public se.michaelthelin.spotify.model_objects.specification.Track getTrack(String trackId) {
        ensureValidToken();

        try {
            GetTrackRequest request = spotifyApi.getTrack(trackId)
                    .build();

            return request.execute();
        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            log.error("Error fetching track details from Spotify: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch track: " + trackId, e);
        }
    }

    /**
     * 토큰이 유효한지 확인하고, 필요시 갱신합니다.
     */
    private void ensureValidToken() {
        if (!authService.isTokenValid()) {
            log.info("Spotify token is invalid or expired. Refreshing...");
            authService.refreshAccessToken();
        }
    }
}
