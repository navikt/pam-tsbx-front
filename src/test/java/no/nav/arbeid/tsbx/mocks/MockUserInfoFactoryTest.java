package no.nav.arbeid.tsbx.mocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MockUserInfoFactoryTest {

    @Test
    public void testGenerateValidPid() {

        assertEquals(11, MockUserInfoFactory.generateValidPid().length());

    }

}
