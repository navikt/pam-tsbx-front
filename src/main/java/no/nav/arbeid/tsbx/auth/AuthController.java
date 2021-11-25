package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
public class AuthController {

    private final UserSession sessionProvider;

    private final IdPortenClient idPortenClient;

    private final IdPortenTokenValidator idTokenValidator;

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserSession sessionProvider,
                          IdPortenClient idPortenClient,
                          IdPortenTokenValidator idTokenValidator) {
        this.sessionProvider = sessionProvider;
        this.idPortenClient = idPortenClient;
        this.idTokenValidator = idTokenValidator;
    }

    /**
     * Step 1 of login flow. Redirects to authorization provider.
     * */
    @GetMapping("/auth/login")
    public ResponseEntity<Void> login() {
        if (sessionProvider.isValid()) {
            return nonCacheableRedirectResponse("/");
        }

        final var authState = sessionProvider.setNewAuthFlowState();
        final var redirectToAuthorization = idPortenClient.buildAuthorizationRequestUri(authState);

        return nonCacheableRedirectResponse(redirectToAuthorization.toASCIIString());
    }

    /**
     * Logout initiated from application.
     * @param request
     * @return
     */
    @GetMapping("/auth/logout")
    public ResponseEntity<Void> applicationLogout(HttpServletRequest request, HttpSession httpSession) {
        if (!sessionProvider.isValid()) {
            return nonCacheableRedirectResponse("/");
        }

        final var idToken = sessionProvider.getIdPortenSessionState()
                .orElseThrow(() -> new IllegalStateException("Session unexpectedly missing id-porten state")).idToken();

        httpSession.invalidate();

        final var idPortenEndSessionUri = idPortenClient.buildEndSessionRequestUri(idToken);
        return nonCacheableRedirectResponse(idPortenEndSessionUri.toASCIIString());
    }

    /**
     * Step 2 of login flow: receive callback request with authorization code from
     * The redirect URI of the client (that is us, the relaying party or RP).
     *
     * @param request
     * @return
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<Void> callback(HttpServletRequest request) {
        final var authState = sessionProvider.getAndRemoveAuthFlowState()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad auth state"));

        try {
            final var requestUrl = request.getRequestURL().append(
                    request.getQueryString() != null ? "?" + request.getQueryString() : "").toString();
            final var authResponse = AuthorizationResponse.parse(new URI(requestUrl));

            // assert state value
            if (! authResponse.getState().equals(authState.getState())) {
                LOG.warn("oauth2 callback: received state did not match session state");
                return ResponseEntity.badRequest().build();
            }

            if (!authResponse.indicatesSuccess()) {
                LOG.warn("oauth2 callback: unsuccessful authorization: {}", authResponse.toErrorResponse().getErrorObject());
                return ResponseEntity.badRequest().build();
            }

            final var successResponse = authResponse.toSuccessResponse();
            final var authorizationCode = successResponse.getAuthorizationCode();

            // Step 3 of login flow, obtain user id-token using code grant
            final var tokenResponse = idPortenClient.fetchCodeGrantToken(authState, authorizationCode);
            final JWT idToken = tokenResponse.getOIDCTokens().getIDToken();

            // Step 4 of login flow: validate id token
            IDTokenClaimsSet validatedClaimsSet = idTokenValidator.validate(idToken, authState.getNonce());

            logReceivedTokens(tokenResponse);

            // Step 5 of login flow, established session for authenticated user, redirect to application user info endpoint
            final var userInfo = new UserInfo(validatedClaimsSet.getSubject().getValue(), validatedClaimsSet.getStringClaim("pid"));
            sessionProvider.setUserInfo(userInfo);
            sessionProvider.setIdPortenSessionState(new IdPortenSessionState(validatedClaimsSet.getStringClaim("sid"), idToken));

            return nonCacheableRedirectResponse("/");

        } catch (URISyntaxException | ParseException | IdPortenTokenValidator.IdPortenTokenValidationException | java.text.ParseException e) {
            LOG.warn("Oauth flow exception for callback request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    private void logReceivedTokens(OIDCTokenResponse tokenResponse) throws java.text.ParseException {
        LOG.info("OAuth token response id_token: issuer={}, expiration={}",
                tokenResponse.getOIDCTokens().getIDToken().getJWTClaimsSet().getIssuer(),
                tokenResponse.getOIDCTokens().getIDToken().getJWTClaimsSet().getExpirationTime());
        LOG.info("OAuth token response access_token={}",
                tokenResponse.getTokens().getAccessToken().toJSONObject());
        LOG.info("OAuth token response refresh_token={}",
                tokenResponse.getTokens().getRefreshToken().toJSONObject());
        LOG.info("OAuth entire token response: " + tokenResponse.toJSONObject());
    }

    /**
     * OIDC front channel logout.
     *
     * Calls to this endpoint come from ID-porten directly, not end user. So end user session is typically not available.
     */
    @GetMapping("/oauth2/logout")
    public ResponseEntity<String> logout(@RequestParam("sid") String idPortenSessionId,
                                         @RequestParam("iss") String issuer) {

        // TODO map idporten session id to local session id and invalidate.
        return ResponseEntity.ok("Sorry, I did nothing with that");
    }

    private ResponseEntity<Void> nonCacheableRedirectResponse(String redirectLocation) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, redirectLocation);
        headers.add(HttpHeaders.CACHE_CONTROL, "no-store, no-cache");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

}
