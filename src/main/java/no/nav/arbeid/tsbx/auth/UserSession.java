package no.nav.arbeid.tsbx.auth;

import java.io.Serializable;
import java.util.Optional;

public class UserSession implements Serializable {

    private UserInfo userInfo;
    private AuthFlowState authFlowState;
    private IdPortenSessionState idPortenSessionState;

    public Optional<UserInfo> getUserInfo() {
        return Optional.ofNullable(userInfo);
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public Optional<IdPortenSessionState> getIdPortenSessionState() {
        return Optional.ofNullable(idPortenSessionState);
    }

    public void setIdPortenSessionState(IdPortenSessionState idPortenSessionState) {
        this.idPortenSessionState = idPortenSessionState;
    }

    public AuthFlowState setNewAuthFlowState() {
        final AuthFlowState instance = new AuthFlowState();
        this.authFlowState = instance;
        return instance;
    }

    public Optional<AuthFlowState> getAndRemoveAuthFlowState() {
        final AuthFlowState instance = this.authFlowState;
        this.authFlowState = null;
        return Optional.ofNullable(instance);
    }

}
