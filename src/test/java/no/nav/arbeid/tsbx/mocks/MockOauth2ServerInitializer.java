package no.nav.arbeid.tsbx.mocks;

import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.OAuth2Config;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Starts Mock Oauth2 server, registers it as bean in context and sets dynamic port as configuration property.
 */
public class MockOauth2ServerInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

    public static final String MOCK_OAUTH2_SERVER_PORT_PROPERTY_NAME = "mock-oauth2-server.port";

    private final boolean interactiveLogin;

    public MockOauth2ServerInitializer() {
        this.interactiveLogin = false;
    }

    public MockOauth2ServerInitializer(boolean interactiveLogin) {
        this.interactiveLogin = interactiveLogin;
    }

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
        try {
            MockOAuth2Server server = new MockOAuth2Server(new OAuth2Config(this.interactiveLogin));
            final int port = SocketUtils.findAvailableTcpPort(50000, 55000);
            server.start(port);

            applicationContext.registerBean(MockOAuth2Server.class,
                    () -> server, def -> def.setDestroyMethodName("shutdown"));

            applicationContext.getEnvironment().getPropertySources().addLast(
                    new MapPropertySource(MockOauth2ServerInitializer.class.getName(),
                    Map.of(MOCK_OAUTH2_SERVER_PORT_PROPERTY_NAME, String.valueOf(port))));
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

}
