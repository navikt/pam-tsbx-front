package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jwt.JWT;

import java.util.Objects;

public record IdPortenSessionState(
        String idPortenSessionId,
        JWT idToken
) {

    public IdPortenSessionState {
        Objects.requireNonNull(idToken);
    }
}
