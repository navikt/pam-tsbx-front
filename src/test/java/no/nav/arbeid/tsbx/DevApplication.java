package no.nav.arbeid.tsbx;

import no.nav.arbeid.tsbx.mocks.MockOauth2ServerInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DevApplication {

    public static void main(String...cmdLineArgs) {
        SpringApplication app = new SpringApplication(DevApplication.class);
        app.setAdditionalProfiles("test");
        app.run(cmdLineArgs);
    }

}
