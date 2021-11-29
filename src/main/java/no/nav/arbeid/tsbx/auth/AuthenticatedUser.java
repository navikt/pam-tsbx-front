package no.nav.arbeid.tsbx.auth;

import java.io.Serializable;
import java.util.Objects;

public record AuthenticatedUser(UserInfo userInfo, IdPortenSession idPortenSession) implements Serializable {
    public AuthenticatedUser {
        Objects.requireNonNull(userInfo);
        Objects.requireNonNull(idPortenSession);
    }
}
