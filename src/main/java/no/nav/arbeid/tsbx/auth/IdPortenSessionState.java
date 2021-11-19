package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jwt.JWT;

public record IdPortenSessionState(
        String idPortenSessionId,
        JWT idToken
) {
}
