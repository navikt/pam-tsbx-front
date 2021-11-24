package no.nav.arbeid.tsbx.messages;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

public class MessagesApiClient {

    private final RestTemplate restTemplate;
    private final URI messagesUrl;

    public MessagesApiClient(URI messagesUrl, RestTemplate restTemplateWithAuthorizationInterceptor) {
        this.restTemplate = restTemplateWithAuthorizationInterceptor;
        this.messagesUrl = messagesUrl;
    }

    public List<Message> getMessagesForCurrentUser() {
        return restTemplate.exchange(RequestEntity
                .get(messagesUrl)
                .accept(MediaType.APPLICATION_JSON)
                .build(), new ParameterizedTypeReference<List<Message>>() {}).getBody();
    }

}
