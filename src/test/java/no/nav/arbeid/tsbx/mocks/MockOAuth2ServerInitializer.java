package no.nav.arbeid.tsbx.mocks;

import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.OAuth2Config;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

/**
 * This configuration code is used to to pre-allocate port and set the canonical hostname of a mock oauth2 server in Spring
 * context config environment.
 *
 * <p>This is necessary so that the dynamic Mock OAuth2 server can be referenced in external security configuration of app, and
 * with an always correct hostname.</p>
 */
public class MockOAuth2ServerInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

    public static final String SERVER_INTERACTIVE_LOGIN_PROP = "mock-oauth2-server.interactiveLogin";
    public static final String SERVER_PORT_PROP = "mock-oauth2-server.port";
    public static final String SERVER_HOSTNAME_PROP = "mock-oauth2-server.hostname";

    private static final Logger LOG = LoggerFactory.getLogger(MockOAuth2ServerInitializer.class);

    @Override
    public void initialize(GenericApplicationContext applicationContext) {

        final var interactiveLogin = applicationContext.getEnvironment()
                .getProperty("mock-oauth2-server.interactiveLogin", Boolean.class, false);

        final var port = applicationContext.getEnvironment()
                .getProperty("mock-oauth2-server.port", Integer.class, SocketUtils.findAvailableTcpPort(10000, 11000));

        final var address = InetAddress.getLoopbackAddress();

        final MapPropertySource propertySource = new MapPropertySource(MockOAuth2ServerInitializer.class.getName(),
                Map.of(
                        SERVER_PORT_PROP, port,
                        SERVER_HOSTNAME_PROP, address.getCanonicalHostName(),
                        SERVER_INTERACTIVE_LOGIN_PROP, interactiveLogin
                ));
        applicationContext.getEnvironment().getPropertySources().addFirst(propertySource);

        LOG.info("Configured mock OAuth2 server properties: {}={}, {}={}, {}={}",
                SERVER_HOSTNAME_PROP, address.getCanonicalHostName(),
                SERVER_PORT_PROP, port,
                SERVER_INTERACTIVE_LOGIN_PROP, interactiveLogin);

        final MockOAuth2Server mockOAuth2Server = createAndStartInstance(address, port, interactiveLogin);
        applicationContext.registerBean(MockOAuth2Server.class,
                () -> mockOAuth2Server, def -> def.setDestroyMethodName("shutdown"));

    }

    private MockOAuth2Server createAndStartInstance(InetAddress address, int port, boolean interactiveLogin) {
        final OAuth2Config oAuth2Config = new OAuth2Config(interactiveLogin,
                null, new OAuth2TokenProvider(), Set.of(new DefaultOAuth2TokenCallback()));
        final MockOAuth2Server mockOAuth2Server = new MockOAuth2Server(oAuth2Config);

        try {
            mockOAuth2Server.start(address, port);
            LOG.info("Started Mock OAuth2 server on {}:{}, interactiveLogin={}",
                    address.getCanonicalHostName(), port, interactiveLogin);
        } catch (BindException b) {
            LOG.warn("Mock OAuth2 server failed bind to port {}, assuming another instance is running.", port);
        } catch (IOException io) {
            throw new RuntimeException("Failed to start Mock OAuth2 server on port " + port, io);
        }

        return mockOAuth2Server;
    }
}
