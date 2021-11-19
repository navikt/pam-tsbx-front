package no.nav.arbeid.tsbx.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

    private final UserSession session;

    public UserInfoController(UserSession session) {
        this.session = session;
    }

    @GetMapping("/user")
    public ResponseEntity getUserInfo() {
        if (session.getUserInfo().isPresent()) {
            return ResponseEntity.ok(session.getUserInfo().get());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sorry, you are not authenticated.");
    }

}
