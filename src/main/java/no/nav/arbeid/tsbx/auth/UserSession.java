package no.nav.arbeid.tsbx.auth;

import java.io.Serializable;
import java.util.Optional;

public class UserSession implements Serializable {

    private UserInfo userInfo;
    private AuthState authState;
    private String idPortenSessionId;

    public Optional<UserInfo> getUserInfo() {
        return Optional.ofNullable(userInfo);
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public Optional<String> getIdPortenSessionId() {
        return Optional.ofNullable(idPortenSessionId);
    }

    public void setIdPortenSessionId(String idPortenSessionId) {
        this.idPortenSessionId = idPortenSessionId;
    }

    public AuthState setNewAuthState() {
        final AuthState instance = new AuthState();
        this.authState = instance;
        return instance;
    }

    public Optional<AuthState> getAndRemoveAuthState() {
        final AuthState instance = this.authState;
        this.authState = null;
        return Optional.ofNullable(instance);
    }

    public boolean isAuthenticated() {
        return getUserInfo().isPresent();
    }

}
