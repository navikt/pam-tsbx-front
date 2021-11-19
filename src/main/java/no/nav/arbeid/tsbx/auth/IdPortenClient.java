package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Token client for authorization code grant type
 * Build URLs for idporten redirects.
 */
public class IdPortenClient {

    private static final Logger LOG = LoggerFactory.getLogger(IdPortenClient.class);

    private final OIDCProviderMetadata oidcProvider;
    private final IdPortenConfigurationProperties idPortenProps;
    private final RSAKey clientKey;

    public IdPortenClient(OIDCProviderMetadata oidcProvider,
                          IdPortenConfigurationProperties idPortenProps) {
        this.oidcProvider = Objects.requireNonNull(oidcProvider);
        this.idPortenProps = Objects.requireNonNull(idPortenProps);
        try {
            this.clientKey = parseClientJwk();
        } catch(ParseException e) {
            throw new IdPortenClientException(e);
        }
    }

    public OIDCTokenResponse fetchCodeGrantToken(AuthFlowState authFlowState, AuthorizationCode authCode) {
        final var callbackUri = idPortenProps.redirectUri();
        final var authorizationCodeGrant = new AuthorizationCodeGrant(authCode, callbackUri, authFlowState.getCodeVerifier());
        final var tokenEndpoint = oidcProvider.getTokenEndpointURI();

        try {
            final var signedJwtClientAuth = generateSignedClientAuth();
            LOG.debug("Client auth claims: {}", signedJwtClientAuth.getClientAssertion().getJWTClaimsSet().toJSONObject(true));

            final var tokenRequest = new TokenRequest(
                    tokenEndpoint,
                    signedJwtClientAuth,
                    authorizationCodeGrant,
                    new Scope(OIDCScopeValue.OPENID));

            final var tokenResponse = tokenRequest.toHTTPRequest().send();
            if (!tokenResponse.indicatesSuccess()) {
                throw new IdPortenClientException("Bad token response status: "
                        + tokenResponse.getStatusCode() + ", content: " + tokenResponse.getContent());
            }

            final var oidcTokenResponse = OIDCTokenResponse.parse(tokenResponse);
            if (!oidcTokenResponse.indicatesSuccess()) {
                throw new IdPortenClientException(oidcTokenResponse.toErrorResponse().getErrorObject().toString());
            }

            return oidcTokenResponse.toSuccessResponse();
        } catch (Exception e) {
            throw new IdPortenClientException(e);
        }
    }

    public URI buildAuthorizationRequestUri(AuthFlowState authFlowState) {
        // The authorisation endpoint of the server
        URI authzEndpoint = oidcProvider.getAuthorizationEndpointURI();

        // The client identifier provisioned by the server
        ClientID clientID = new ClientID(idPortenProps.clientId());

        // The client callback URI, typically pre-registered with the server
        URI callback = idPortenProps.redirectUri();

        // Build the request URI
        return new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE), clientID)
                .scope(new Scope(OIDCScopeValue.OPENID))
                .resource(idPortenProps.clientUri())
                .state(authFlowState.getState())
                .customParameter("nonce", authFlowState.getNonce().getValue())
                .codeChallenge(authFlowState.getCodeVerifier(), CodeChallengeMethod.S256)
                .redirectionURI(callback)
                .endpointURI(authzEndpoint)
                .build()
                .toURI();
    }

    public URI buildEndSessionRequestUri(JWT idToken) {
        LogoutRequest logoutRequest = new LogoutRequest(
                oidcProvider.getEndSessionEndpointURI(),
                idToken,
                idPortenProps.postLogoutUri(),
                null);
        return logoutRequest.toURI();
    }

    private RSAKey parseClientJwk() throws ParseException {
        String jsonValue = idPortenProps.clientJwk();
        return JWK.parse(jsonValue).toRSAKey();
    }

    private PrivateKeyJWT generateSignedClientAuth() throws JOSEException {
        final ClientID clientID = new ClientID(idPortenProps.clientId());
        final Instant now = Instant.now();
        final Date iat = Date.from(now);
        final Date exp = Date.from(now.plusSeconds(30));
        final List<Audience> aud = List.of(new Audience(oidcProvider.getIssuer()));

        final JWTAuthenticationClaimsSet jwtAuthenticationClaimsSet = new JWTAuthenticationClaimsSet(
                clientID, aud, exp, null, iat, new JWTID());

        return new PrivateKeyJWT(jwtAuthenticationClaimsSet,
                JWSAlgorithm.RS256,
                clientKey.toRSAPrivateKey(),
                clientKey.getKeyID(),
                null);
    }

    public static class IdPortenClientException extends RuntimeException {
        public IdPortenClientException(String message) {
            super(message);
        }

        public IdPortenClientException(Throwable cause) {
            super(cause);
        }
    }
}
