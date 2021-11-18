package no.nav.arbeid.tsbx.config;

import ch.qos.logback.access.tomcat.LogbackValve;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;

@Configuration
public class AccessLogConfiguration {

    @Bean
    public TomcatContextCustomizer setupLogbackWithTomcat(@Value("${logback.access.config-resource}") String configResource) {
        return (context) -> {
            try (var configStream = context.getClass().getResourceAsStream(configResource)) {
                Files.createDirectories(context.getCatalinaBase().toPath().resolve(LogbackValve.DEFAULT_CONFIG_FILE).getParent());
                Files.copy(configStream, context.getCatalinaBase().toPath().resolve(LogbackValve.DEFAULT_CONFIG_FILE));
            } catch (IOException io) {
                throw new RuntimeException("Failed to copy logback-access.xml config into Tomcat server context", io);
            }

            final var valve = new LogbackValve();
            valve.setQuiet(true);
            context.getPipeline().addValve(valve);
        };
    }

}
