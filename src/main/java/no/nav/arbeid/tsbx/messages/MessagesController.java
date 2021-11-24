package no.nav.arbeid.tsbx.messages;

import no.nav.arbeid.tsbx.auth.UserSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
        try {
            sessionProvider.getIfValid();
        } catch (UserSession.InvalidSessionException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        return ResponseEntity.ok(messagesApiClient.getMessagesForCurrentUser());
    }

}
