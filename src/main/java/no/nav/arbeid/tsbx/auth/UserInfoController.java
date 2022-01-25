package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jose.Header;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
public class UserInfoController {

    private final UserSession session;

    public UserInfoController(UserSession session) {
        this.session = session;
    }

    @GetMapping("/user")
    public ResponseEntity<UserInfo> getUserInfo() {
        return ResponseEntity.ok(session.authenticatedUser().map(AuthenticatedUser::userInfo).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

    @GetMapping(value = "/user/idtoken/header", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getIdTokenHeader() {
        return ResponseEntity.ok(
                session.authenticatedUser()
                        .map(AuthenticatedUser::idPortenSession)
                        .map(IdPortenSession::idToken)
                        .map(JWT::getHeader)
                        .map(Header::toString)
                .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

    @GetMapping(value = "/user/idtoken/claims", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getIdTokenClaims() {
        return ResponseEntity.ok(
                session.authenticatedUser()
                        .map(AuthenticatedUser::idPortenSession)
                        .map(IdPortenSession::idToken)
                        .flatMap(jwt -> {
                            try { return Optional.of(jwt.getJWTClaimsSet().toString()); }
                            catch (java.text.ParseException pe) { return Optional.empty(); }
                        })
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

}
