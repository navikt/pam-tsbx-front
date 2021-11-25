package no.nav.arbeid.tsbx.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

}
