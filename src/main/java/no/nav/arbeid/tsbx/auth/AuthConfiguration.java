package no.nav.arbeid.tsbx.auth;

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

@EnableConfigurationProperties(IdPortenConfigurationProperties.class)
@Configuration
public class AuthConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AuthConfiguration.class);

    @Bean
    public OIDCProviderMetadata idPortenMetadata(IdPortenConfigurationProperties idPortenConfig) throws IOException, ParseException {
        Issuer issuer = new Issuer(idPortenConfig.wellKnownUrl());
        HTTPResponse response = new OIDCProviderConfigurationRequest(issuer).toHTTPRequest().send();

        // todo log well known metadata response
        LOG.info("OIDC issuer well known metadata:");
        LOG.info(response.getContent());

        return OIDCProviderMetadata.parse(response.getContent());
    }

    @Bean
    @SessionScope
    public UserSession userSession() {
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
}
