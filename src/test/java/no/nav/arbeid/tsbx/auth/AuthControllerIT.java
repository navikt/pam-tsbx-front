package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jose.JOSEObjectType;
import no.nav.arbeid.tsbx.Application;
import no.nav.arbeid.tsbx.mocks.MockOauth2ServerInitializer;
import no.nav.arbeid.tsbx.mocks.MockUserInfoFactory;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption.ENABLE_COOKIES;
import static org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption.ENABLE_REDIRECTS;


@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = { MockOauth2ServerInitializer.class })
@ExtendWith(SpringExtension.class)
public class AuthControllerIT {

    private static final Logger LOG = LoggerFactory.getLogger(AuthControllerIT.class);

    private final TestRestTemplate restTemplate;

    @Autowired
    public AuthControllerIT(Environment env) {
        this.restTemplate =  new TestRestTemplate(ENABLE_REDIRECTS, ENABLE_COOKIES);
        restTemplate.setUriTemplateHandler(new LocalHostUriTemplateHandler(env));
    }

    @LocalServerPort
    private int port;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockOAuth2Server mockOAuth2Server;

    private String localServerUrl() {
        return "http://localhost:" + port;
    }

    @Test
    public void testRequireAuth() {
        ResponseEntity<String> userInfo = restTemplate.exchange(RequestEntity.get("/user").build(), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, userInfo.getStatusCode());
    }

    @Test
    public void testIdPortenLoginAuthorizationCodeFlow() {
        final var expectedUserInfo = MockUserInfoFactory.generateRandomUser();

        mockOAuth2Server.enqueueCallback(new DefaultOAuth2TokenCallback("idporten", expectedUserInfo.name(),
                JOSEObjectType.JWT.getType(), null, Map.of(
                "acr", "Level3",
                "pid", expectedUserInfo.pid())));


        ResponseEntity<Void> loginFlowResponse = restTemplate.exchange(
                RequestEntity.get(localServerUrl() + "/auth/login")
                        .accept(MediaType.TEXT_HTML)
                        .build(), Void.class);

        assertEquals(HttpStatus.OK, loginFlowResponse.getStatusCode());

        ResponseEntity<UserInfo> userInfoResponse = restTemplate.exchange(
                RequestEntity.get(localServerUrl() + "/user").accept(MediaType.APPLICATION_JSON).build(), UserInfo.class);

        assertEquals(expectedUserInfo.name(), userInfoResponse.getBody().name());
        assertEquals(expectedUserInfo.pid(), userInfoResponse.getBody().pid());

        LOG.info("Login using authorization code flow successful as {}", userInfoResponse.getBody().name());
    }

    @Test
    public void testLogout() {
        final var userInfo = MockUserInfoFactory.generateRandomUser();

        mockOAuth2Server.enqueueCallback(new DefaultOAuth2TokenCallback("idporten", userInfo.name(),
                JOSEObjectType.JWT.getType(), null, Map.of(
                "acr", "Level3",
                "pid", userInfo.pid())));

        ResponseEntity<Void> response = restTemplate.exchange(RequestEntity.get(localServerUrl() + "/auth/login")
                .accept(MediaType.TEXT_HTML).build(), Void.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(RequestEntity.get("/auth/logout")
                .accept(MediaType.TEXT_HTML)
                .build(), Void.class);
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());

        ResponseEntity<String> userInfoResponse = restTemplate.exchange(RequestEntity.get("/user")
                .accept(MediaType.TEXT_PLAIN)
                .build(), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, userInfoResponse.getStatusCode());
    }

    @Test
    public void testFrontChannelLogout() {
        final var userInfo = MockUserInfoFactory.generateRandomUser();

        mockOAuth2Server.enqueueCallback(new DefaultOAuth2TokenCallback("idporten", userInfo.name(),
                JOSEObjectType.JWT.getType(), null, Map.of(
                "acr", "Level3",
                "sid", "testsid",
                "pid", userInfo.pid())));

        ResponseEntity<Void> response = restTemplate.exchange(RequestEntity.get(localServerUrl() + "/auth/login")
                .accept(MediaType.TEXT_HTML).build(), Void.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Simulate front channel logout call
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(RequestEntity.get("/oauth2/logout?sid=testsid&iss={iss}",
                        mockOAuth2Server.issuerUrl("idporten")).build(), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());

        ResponseEntity<String> userInfoResponse = restTemplate.exchange(RequestEntity.get("/user")
                .accept(MediaType.TEXT_PLAIN)
                .build(), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, userInfoResponse.getStatusCode());
    }

    @Test
    public void testFrontChannelLogout_noSessionAvailable() {
        final var userInfo = MockUserInfoFactory.generateRandomUser();

        mockOAuth2Server.enqueueCallback(new DefaultOAuth2TokenCallback("idporten", userInfo.name(),
                JOSEObjectType.JWT.getType(), null, Map.of(
                "acr", "Level3",
                "sid", "sidvalue",
                "pid", userInfo.pid())));

        ResponseEntity<Void> response = restTemplate.exchange(RequestEntity.get(localServerUrl() + "/auth/login")
                .accept(MediaType.TEXT_HTML).build(), Void.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Simulate front channel logout call without cookies included
        TestRestTemplate otherClient = new TestRestTemplate();
        ResponseEntity<Void> logoutResponse = otherClient.exchange(RequestEntity.get(
                restTemplate.getRootUri() + "/oauth2/logout?sid=sidvalue&iss={iss}",
                mockOAuth2Server.issuerUrl("idporten"))
                .build(), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());

        ResponseEntity<String> userInfoResponse = restTemplate.exchange(RequestEntity.get("/user")
                .accept(MediaType.TEXT_PLAIN)
                .build(), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, userInfoResponse.getStatusCode());
    }

}
