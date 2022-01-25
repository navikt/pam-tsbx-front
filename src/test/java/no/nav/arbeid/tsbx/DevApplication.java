package no.nav.arbeid.tsbx;

import no.nav.arbeid.tsbx.mocks.MockOAuth2ServerInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collection;
import java.util.Map;

@SpringBootApplication
public class DevApplication {

    /**
     * The standardized port which runs Mock Oauth2 server when running app locally.
     */
    public static final int DEV_MOCK_OAUTH2_SERVER_PORT = 19111;

    public static void main(String...cmdLineArgs) {
        SpringApplication app = new SpringApplication(DevApplication.class);
        app.setAdditionalProfiles("test");
        app.setDefaultProperties(Map.of(
                "mock-oauth2-server.port", DEV_MOCK_OAUTH2_SERVER_PORT,
                "mock-oauth2-server.interactiveLogin", true
                ));
        app.addInitializers(new MockOAuth2ServerInitializer());
        app.run(cmdLineArgs);
    }

}
