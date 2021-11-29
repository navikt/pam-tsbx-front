package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Optional;

@RestController
public class AuthController {

    private final UserSession sessionProvider;

    private final IdPortenClient idPortenClient;

    private final IdPortenTokenValidator idTokenValidator;

    private final IdPortenFrontChannelLogoutEventStore frontChannelLogoutEventStore;

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserSession sessionProvider,
                          IdPortenClient idPortenClient,
                          IdPortenTokenValidator idTokenValidator,
                          IdPortenFrontChannelLogoutEventStore frontChannelLogoutEventStore) {
        this.sessionProvider = sessionProvider;
        this.idPortenClient = idPortenClient;
        this.idTokenValidator = idTokenValidator;
        this.frontChannelLogoutEventStore = frontChannelLogoutEventStore;
    }

    /**
     * Step 1 of login flow. Redirects to authorization provider.
     * */
    @GetMapping("/auth/login")
    public ResponseEntity<Void> login() {
        if (sessionProvider.authenticatedUser().isPresent()) {
            return nonCacheableRedirectResponse("/");
        }

        final var authState = sessionProvider.setNewAuthCodeFlowState();
        final var redirectToAuthorization = idPortenClient.buildAuthorizationRequestUri(authState);

        return nonCacheableRedirectResponse(redirectToAuthorization.toASCIIString());
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
        final var authState = sessionProvider.getAndRemoveAuthCodeFlowState()
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

            // Step 3 of login flow, obtain end user authorization tokens using code grant
            final var tokenResponse = idPortenClient.fetchCodeGrantToken(authState, authorizationCode);
            final JWT idToken = tokenResponse.getOIDCTokens().getIDToken();
            final AccessToken accessToken = tokenResponse.getOIDCTokens().getAccessToken();
            final RefreshToken refreshToken = tokenResponse.getOIDCTokens().getRefreshToken();

            // Step 4 of login flow: validate id token
            IDTokenClaimsSet validatedClaimsSet = idTokenValidator.validate(idToken, authState.getNonce());

            // Step 5 of login flow, established session for authenticated user, redirect back to application main page
            final var userInfo = new UserInfo(validatedClaimsSet.getSubject().getValue(), validatedClaimsSet.getStringClaim("pid"));
            final var idPortenSession = new IdPortenSession(
                    validatedClaimsSet.getStringClaim("sid"),
                    idToken,
                    accessToken,
                    refreshToken,
                    Instant.now().plusSeconds(accessToken.getLifetime()));

            sessionProvider.setAuthenticatedUser(new AuthenticatedUser(userInfo, idPortenSession));

            return nonCacheableRedirectResponse("/");

        } catch (URISyntaxException | ParseException | IdPortenTokenValidator.IdPortenTokenValidationException e) {
            LOG.warn("Oauth flow exception for callback request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Logout initiated from <em>this</em> application.
     *
     * @return a response redirecting end user to OP endsession endpoint.
     */
    @GetMapping("/auth/logout")
    public ResponseEntity<Void> applicationLogout() {
        if (!sessionProvider.authenticatedUser().isPresent()) {
            return nonCacheableRedirectResponse("/");
        }

        final var idToken = sessionProvider.authenticatedUser().map(a -> a.idPortenSession().idToken())
                .orElseThrow(() -> new IllegalStateException("Session unexpectedly missing id-porten state"));

        sessionProvider.invalidate();

        final var idPortenEndSessionUri = idPortenClient.buildEndSessionRequestUri(idToken);
        return nonCacheableRedirectResponse(idPortenEndSessionUri.toASCIIString());
    }

    /**
     * OIDC front channel logout (logout initiated from some other app sharing the same OP session).
     *
     * Calls to this endpoint is typically initiated and orchestrated by OP, but actual requests come from end user's
     * browser. However, we cannot rely on our local session cookie being sent together with such requests due to
     * browser security policies.
     */
    @GetMapping("/oauth2/logout")
    public ResponseEntity<Void> logout(@RequestParam("sid") String requestSid,
                                       @RequestParam("iss") String requestIssuer) {
        if (!idPortenClient.isKnownIssuer(new Issuer(requestIssuer))) {
            LOG.warn("Bad issuer for front channel logout event");
            return ResponseEntity.badRequest().build();
        }

        // We may not always have local session context available because there is no guarantee the browser includes session cookie
        // for these requests.
        final Optional<String> sessionSid = sessionProvider.authenticatedUser()
                .map(AuthenticatedUser::idPortenSession)
                .map(IdPortenSession::idPortenSessionIdentifier);

        if (sessionSid.isPresent()) {
            if (requestSid.equals(sessionSid.get())) {
                sessionProvider.invalidate();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            // Local session context not available, register the event for possible later invalidation
            frontChannelLogoutEventStore.registerLogout(requestSid);
            LOG.info("No local session found for front channel logout event, sid {}, issuer {}", requestSid, requestIssuer);
        }

        return nonCacheableResponse(HttpStatus.NO_CONTENT).build();
    }

    private ResponseEntity.BodyBuilder nonCacheableResponse(HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-store, no-cache");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        return ResponseEntity.status(status).headers(headers);
    }

    private ResponseEntity<Void> nonCacheableRedirectResponse(String redirectLocation) {
        return nonCacheableResponse(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectLocation).build();
    }

}
