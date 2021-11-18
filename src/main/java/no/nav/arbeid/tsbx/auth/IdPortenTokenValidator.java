package no.nav.arbeid.tsbx.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import java.net.MalformedURLException;

/**
 * Validator for ID-porten id_token.
 * Checks ACR and "pid" claims, in addition to the normal OIDC JWT validation.
 */
public class IdPortenTokenValidator {

    private final IDTokenValidator idTokenValidator;

    public IdPortenTokenValidator(IdPortenConfigurationProperties idPortenProps,
                                  OIDCProviderMetadata oidcProviderMetadata) throws MalformedURLException {
        this.idTokenValidator = new IDTokenValidator(oidcProviderMetadata.getIssuer(),
                new ClientID(idPortenProps.clientId()),
                JWSAlgorithm.RS256,
                oidcProviderMetadata.getJWKSetURI().toURL());
    }

    public IDTokenClaimsSet validate(JWT idToken, Nonce expectedNonce) {
        try {
            IDTokenClaimsSet claimsSet = idTokenValidator.validate(idToken, expectedNonce);
            validateAcr(claimsSet);
            validatePid(claimsSet);

            return claimsSet;
        } catch (BadJOSEException | JOSEException e) {
            throw new IdPortenTokenValidationException(e);
        }
    }

    private void validateAcr(IDTokenClaimsSet claimsSet) {
        final String acrValue = claimsSet.getACR() != null ? claimsSet.getACR().getValue() : null;
        if (! ("Level3".equals(acrValue) || "Level4".equals(acrValue))) {
            throw new IdPortenTokenValidationException("ACR value not valid: " + acrValue);
        }
    }

    private void validatePid(IDTokenClaimsSet claimsSet) {
        final String pidValue = claimsSet.getStringClaim("pid");
        if (pidValue == null || !pidValue.matches("[0-9]{11}")) { // Expect Norwegian NIN-form
            throw new IdPortenTokenValidationException("Invalid person identifier: " + pidValue);
        }
    }

    public static class IdPortenTokenValidationException extends RuntimeException {
        public IdPortenTokenValidationException(String message) {
            super(message);
        }
        public IdPortenTokenValidationException(Throwable cause) {
            super(cause);
        }
    }
}
