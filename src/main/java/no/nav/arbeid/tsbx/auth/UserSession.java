package no.nav.arbeid.tsbx.auth;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class UserSession implements Serializable {

    private AuthCodeFlowState authCodeFlowState;
    private AuthenticatedUser authenticatedUser;

    /**
     * @return authenticated user from session, empty optional if no authenticated user set or access token
     * has expired.
     */
    public Optional<AuthenticatedUser> authenticatedUser() {
        return Optional.ofNullable(authenticatedUser).filter(a -> !a.idPortenSession().isAccessTokenExpired());
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = Objects.requireNonNull(authenticatedUser);
    }

    public AuthCodeFlowState setNewAuthCodeFlowState() {
        final AuthCodeFlowState instance = new AuthCodeFlowState();
        this.authCodeFlowState = instance;
        return instance;
    }

    public Optional<AuthCodeFlowState> getAndRemoveAuthCodeFlowState() {
        final AuthCodeFlowState instance = this.authCodeFlowState;
        this.authCodeFlowState = null;
        return Optional.ofNullable(instance);
    }

}
