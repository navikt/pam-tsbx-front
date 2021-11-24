package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

@EnableConfigurationProperties(IdPortenConfigurationProperties.class)
@Configuration
public class AuthConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AuthConfiguration.class);

    @Bean
    public OIDCProviderMetadata idPortenMetadata(IdPortenConfigurationProperties idPortenConfig) throws IOException, ParseException {
        logIdPortenConfig(idPortenConfig);

        // Nimbus library requires provider URL without the .well-known/openid... suffix appended
        URI issuerUrl = idPortenConfig.wellKnownUrl().resolve("..");
        Issuer issuer = new Issuer(issuerUrl);
        HTTPResponse response = new OIDCProviderConfigurationRequest(issuer).toHTTPRequest().send();
        OIDCProviderMetadata metadata = OIDCProviderMetadata.parse(response.getContent());

        logOidcProviderMetadata(metadata);

        return metadata;
    }

    @Bean
    @SessionScope
    public UserSession userSessionProvider() {
        return new UserSession();
    }

    @Bean
    public IdPortenClient oidcTokenClient(OIDCProviderMetadata oidcProviderMetadata, IdPortenConfigurationProperties idPortenProps) {
        return new IdPortenClient(oidcProviderMetadata, idPortenProps);
    }

    @Bean
    public IdPortenTokenValidator idPortenIdTokenValidator(IdPortenConfigurationProperties idPortenProps,
                                                           OIDCProviderMetadata metadata) throws MalformedURLException {
        return new IdPortenTokenValidator(idPortenProps, metadata);
    }

    private void logIdPortenConfig(IdPortenConfigurationProperties idPortenConfigurationProperties) {
        LOG.info("ID-porten client configuration: clientUri: {}, clientId: {}, redirectUri: {}, well-known: {}",
                idPortenConfigurationProperties.clientUri(), idPortenConfigurationProperties.clientId(),
                idPortenConfigurationProperties.redirectUri(), idPortenConfigurationProperties.wellKnownUrl());

        try {
            JWK clientJwk = JWK.parse(idPortenConfigurationProperties.clientJwk());
            LOG.debug("Client JWK key id: {}", clientJwk.getKeyID());
        } catch (java.text.ParseException pe) {
            LOG.warn("Failed to parse client JWK", pe);
        }
    }

    private void logOidcProviderMetadata(OIDCProviderMetadata metadata) {
        LOG.info("OIDC provider metadata: authorize endpoint: {}, token endpoint: {}, JWKs endpoint: {}",
                metadata.getAuthorizationEndpointURI(), metadata.getTokenEndpointURI(), metadata.getJWKSetURI());
    }
}
