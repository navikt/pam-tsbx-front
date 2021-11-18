package no.nav.arbeid.tsbx.mocks;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class JwkGenerator {

    @Test
    public void generateJwkToSystemOut() throws JOSEException {
        // Generate 2048-bit RSA key pair in JWK format, attach some metadata
        RSAKey jwk = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();

        // Output the private and public RSA JWK parameters
        System.out.println("Private JWK:");
        System.out.println(jwk);

        // Output the public RSA JWK parameters only
        System.out.println("Public JWK:");
        System.out.println(jwk.toPublicJWK());
    }
}
