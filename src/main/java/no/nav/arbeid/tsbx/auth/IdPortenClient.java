package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

    public OIDCTokenResponse fetchCodeGrantToken(AuthState authState, AuthorizationCode authCode) {

        final var callbackUri = idPortenProps.redirectUri();
        final var authorizationCodeGrant = new AuthorizationCodeGrant(authCode, callbackUri, authState.getCodeVerifier());
        final var tokenEndpoint = oidcProvider.getTokenEndpointURI();

        try {
            final var clientAuth = generateSignedClientAuth();

            LOG.info("Client auth claims: {}", clientAuth.getClientAssertion().getJWTClaimsSet().toJSONObject(true));

            final var tokenRequest = new TokenRequest(
                    tokenEndpoint,
                    clientAuth,
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

    public URI buildAuthorizationRequestUri(AuthState authState) {
        // The authorisation endpoint of the server
        URI authzEndpoint = oidcProvider.getAuthorizationEndpointURI();

        // The client identifier provisioned by the server
        ClientID clientID = new ClientID(idPortenProps.clientId());

        // The client callback URI, typically pre-registered with the server
        URI callback = idPortenProps.redirectUri();

        // Build the request URI
        return new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), clientID)
                .scope(new Scope("openid"))
                .resource(idPortenProps.clientUri())
                .state(authState.getState())
                .customParameter("nonce", authState.getNonce().getValue())
                .codeChallenge(authState.getCodeVerifier(), CodeChallengeMethod.S256)
                .redirectionURI(callback)
                .endpointURI(authzEndpoint)
                .build()
                .toURI();
    }

    public URI getEndSessionRequestUri() {
        return oidcProvider.getEndSessionEndpointURI();
    }

    private RSAKey parseClientJwk() throws ParseException {
        String jsonValue = idPortenProps.clientJwk();
        return JWK.parse(jsonValue).toRSAKey();
    }

    private PrivateKeyJWT generateSignedClientAuth() throws JOSEException {
        final ClientID clientID = new ClientID(idPortenProps.clientId());
        final Date exp = Date.from(Instant.now().plus(5, ChronoUnit.MINUTES));
        final List<Audience> audienceList = List.of(new Audience(oidcProvider.getIssuer()));

        final JWTAuthenticationClaimsSet jwtAuthenticationClaimsSet = new JWTAuthenticationClaimsSet(
                clientID, audienceList, exp, null, null, new JWTID());

        return new PrivateKeyJWT(jwtAuthenticationClaimsSet,
                JWSAlgorithm.RS256,
                clientKey.toRSAPrivateKey(),
                null,
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
