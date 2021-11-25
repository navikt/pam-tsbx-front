package no.nav.arbeid.tsbx.messages;

import no.nav.arbeid.tsbx.auth.UserSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fetch messages for user from pam-tsbx-api, on behalf of user.
 *
 * "Protected" by user session validation.
 */
@RestController
public class MessagesController {

    private final UserSession sessionProvider;
    private final MessagesApiClient messagesApiClient;

    public MessagesController(UserSession sessionProvider, MessagesApiClient messagesApiClient) {
        this.sessionProvider = sessionProvider;
        this.messagesApiClient = messagesApiClient;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Message>> getMessages() {
        if (!sessionProvider.authenticatedUser().isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(messagesApiClient.getMessagesForCurrentUser());
    }

}
