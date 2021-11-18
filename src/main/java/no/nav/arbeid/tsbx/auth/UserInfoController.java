package no.nav.arbeid.tsbx.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserInfoController {

    private final UserSession userSession;

    public UserInfoController(UserSession userSession) {
        this.userSession = userSession;
    }

    @GetMapping("/user")
    public ResponseEntity getUserInfo() {
        if (userSession.getUserInfo().isPresent()) {
            return ResponseEntity.ok(userSession.getUserInfo().get());
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Sorry, you are not authenticated.");
    }

}
