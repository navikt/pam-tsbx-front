package no.nav.arbeid.tsbx.messages;

import com.nimbusds.jwt.JWT;
import no.nav.arbeid.tsbx.auth.UserSession;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableOAuth2Client
public class MessagesApiClientConfiguration {

    @Bean
    public MessagesApiClient messagesApiClient(@Value("${pam-tsbx-api.messagesUrl}") URI messagesUrl,
                                               ClientConfigurationProperties clientConfigurationProperties,
                                               OAuth2AccessTokenService oAuth2AccessTokenService,
                                               RestTemplateBuilder restTemplateBuilder) {

        final ClientProperties clientProperties =
                Optional.ofNullable(clientConfigurationProperties.getRegistration().get("pam-tsbx-api"))
                        .orElseThrow(() -> new RuntimeException("could not find oauth2 client config for pam-tsbx-api"));

        final RestTemplate restTemplate = restTemplateBuilder.additionalInterceptors((request, body, execution) -> {
            OAuth2AccessTokenResponse response = oAuth2AccessTokenService.getAccessToken(clientProperties);
            request.getHeaders().setBearerAuth(response.getAccessToken());
            return execution.execute(request, body);
        }).build();

        return new MessagesApiClient(messagesUrl, restTemplate);
    }

    /**
     * This bridges how token-support acquires JWT id-tokens from the user session in this demo app,
     * for use in token exchange requests.
     *
     * @param userSession the user session accessor bean
     * @return a token validation context holder which looks up token in user session
     */
    @Bean
    public TokenValidationContextHolder tokenValidationContextHolder(UserSession userSession) {
        return new TokenValidationContextHolder() {
            @Override
            public TokenValidationContext getTokenValidationContext() {
                final Optional<JWT> idToken = userSession.getIdPortenSessionState().map(idpss -> idpss.idToken());
                if (!idToken.isPresent()) {
                    return null;
                }

                return new TokenValidationContext(Map.of("idporten", new JwtToken(idToken.get().getParsedString())));
            }

            @Override
            public void setTokenValidationContext(TokenValidationContext tokenValidationContext) {
            }
        };
    }

}
