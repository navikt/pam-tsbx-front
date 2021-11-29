package no.nav.arbeid.tsbx.auth;

import java.io.Serializable;
import java.util.Random;

public record UserInfo(String name, String pid) implements Serializable {

    public String name() {
        if (name.endsWith("=")) {
            // Likely ID-porten subject identifier, convert to a fake name that consistently follows from it.
            return generateNameFromOpaqueSubjectIdentifier(name);
        }
        return name;
    }

    /**
     * ID-porten does not provide actual names in ID-tokens, så make up some names here, based on opaque subject identifier.
     * In a real app, this data might come from PDL.
     */
    private static String generateNameFromOpaqueSubjectIdentifier(String id) {
        final Random random = new Random(id.hashCode());
        final String[] firstNames = {
                "Nora", "Jakob",
                "Emma", "Emil",
                "Ella", "Noah",
                "Maja", "Oliver",
                "Olivia", "Filip",
                "Emilie", "William",
                "Sofie", "Lukas",
                "Leah", "Liam",
                "Sofia", "Henrik",
                "Ingrid", "Oskar"
        };
        final String[] lastNames = {
                "Hansen",
                "Johansen",
                "Olsen",
                "Larsen",
                "Andersen",
                "Pedersen",
                "Nilsen",
                "Kristiansen",
                "Jensen",
                "Karlsen",
                "Johnsen",
                "Pettersen",
                "Eriksen",
                "Berg",
                "Haugen",
                "Hagen",
                "Johannessen",
                "Andreassen"
        };

        return firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)];
    }

}
