package no.nav.arbeid.tsbx.auth;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.Nonce;

import java.io.Serializable;

/**
 * A single AuthFlowState instance should only be used for a single OIDC authorization code transaction.
 * It is for the most part immutable.
 * Do not reuse across different authentication attempts.
 */
public class AuthFlowState implements Serializable {

    private final State state;
    private final Nonce nonce;
    private final CodeVerifier codeVerifier;

    public AuthFlowState() {
        state = new State();
        nonce = new Nonce();
        codeVerifier = new CodeVerifier();
    }

    public State getState() {
        return state;
    }

    public Nonce getNonce() {
        return nonce;
    }

    public CodeVerifier getCodeVerifier() {
        return codeVerifier;
    }
}
