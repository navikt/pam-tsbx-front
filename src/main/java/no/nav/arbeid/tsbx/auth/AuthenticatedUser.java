package no.nav.arbeid.tsbx.auth;

import java.util.Objects;

public record AuthenticatedUser(UserInfo userInfo, IdPortenSession idPortenSession) {
    public AuthenticatedUser {
        Objects.requireNonNull(userInfo);
        Objects.requireNonNull(idPortenSession);
    }
}
