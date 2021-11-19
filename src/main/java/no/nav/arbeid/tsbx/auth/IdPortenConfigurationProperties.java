package no.nav.arbeid.tsbx.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.net.URI;
import java.util.Objects;

/**
 * Typically values for these properties are injected into app by nais platform, which handles registration of client
 * app at the identity provider.
 */
@ConfigurationProperties(prefix = "idporten")
@ConstructorBinding
public record IdPortenConfigurationProperties(
        URI clientUri,
        URI postLogoutUri,
        String clientId,
        String clientJwk,
        URI redirectUri,
        URI wellKnownUrl
) {
    public IdPortenConfigurationProperties {
        Objects.requireNonNull(clientUri);
        Objects.requireNonNull(postLogoutUri);
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(clientJwk);
        Objects.requireNonNull(redirectUri);
        Objects.requireNonNull(wellKnownUrl);
    }
}
