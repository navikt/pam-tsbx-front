package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public record IdPortenSession(
        String idPortenSessionIdentifier,
        JWT idToken,
        AccessToken accessToken,
        RefreshToken refreshToken,
        Instant accessTokenExpiry
) {
    public IdPortenSession {
        Objects.requireNonNull(idToken);
        Objects.requireNonNull(accessToken);
        Objects.requireNonNull(refreshToken);
        Objects.requireNonNull(accessTokenExpiry);
    }

    /**
     *
     * @return {@code true} if {@link #accessToken()} has expired.
     */
    public boolean isAccessTokenExpired() {
        return Instant.now().isAfter(accessTokenExpiry);
    }

}
