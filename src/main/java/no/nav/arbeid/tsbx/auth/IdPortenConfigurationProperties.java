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
        String clientId,
        String clientJwk,
        URI redirectUri,
        URI wellKnownUrl
) {
    /**
     *
     * @param clientId client id as string
     * @param clientJwk client JWK as a literal JSON-formatted string, RSA private key type.
     * @param redirectUri where should idporten redirect users after authentication ?
     * @param wellKnownUrl OIDC "well known" metadata endpoint
     */
    public IdPortenConfigurationProperties {
        Objects.requireNonNull(clientUri);
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(clientJwk);
        Objects.requireNonNull(redirectUri);
        Objects.requireNonNull(wellKnownUrl);
    }
}
