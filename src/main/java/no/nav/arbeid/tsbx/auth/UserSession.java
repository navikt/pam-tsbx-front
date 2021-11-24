package no.nav.arbeid.tsbx.auth;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
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

    public UserSession checkValid() {
        if (userInfo == null) {
            throw new InvalidSessionException("No user authenticated");
        }
        if (idPortenSessionState == null) {
            throw new InvalidSessionException("Missing idporten session state");
        }
        try {
            if (idPortenSessionState.idToken().getJWTClaimsSet().getExpirationTime().before(new Date())) {
                throw new InvalidSessionException("id token is expired");
            }
        } catch (ParseException e) {
            throw new InvalidSessionException("id token invalid");
        }
        return this;
    }

    public static class InvalidSessionException extends RuntimeException {
        public InvalidSessionException(String message) {
            super(message);
        }
    }

}
