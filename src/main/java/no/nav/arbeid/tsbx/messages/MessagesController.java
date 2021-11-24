package no.nav.arbeid.tsbx.messages;

import no.nav.arbeid.tsbx.auth.UserSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Fetch messages for user from pam-tsbx-api, on behalf of user.
 *
 * "Protected" by user session validation.
 */
@RestController
public class MessagesController {

    private final UserSession userSession;
    private final MessagesApiClient messagesApiClient;

    public MessagesController(UserSession userSession, MessagesApiClient messagesApiClient) {
        this.userSession = userSession;
        this.messagesApiClient = messagesApiClient;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Message>> getMessages() {
        final UserSession session;
        try {
            session = userSession.checkValid();
        } catch (UserSession.InvalidSessionException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        return ResponseEntity.ok(messagesApiClient.getMessagesForCurrentUser());
    }

}
