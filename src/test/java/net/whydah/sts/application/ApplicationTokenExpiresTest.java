package net.whydah.sts.application;

import net.whydah.sso.commands.systemtestbase.SystemTestBaseConfig;
import net.whydah.sso.ddd.model.application.ApplicationTokenExpires;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.whydah.sts.application.AuthenticatedApplicationTokenRepository.DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS;
import static net.whydah.sts.application.AuthenticatedApplicationTokenRepository.STS_TOKEN_MULTIPLIER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationTokenExpiresTest {

    private static final Logger log = LoggerFactory.getLogger(ApplicationFullTokenTest.class);

    static SystemTestBaseConfig config;

    @BeforeClass
    public static void setup() throws Exception {
        config = new SystemTestBaseConfig();
    }

    @Test
    public void testDefaultValues() {
        assertTrue(ApplicationTokenExpires.isValid(System.currentTimeMillis() + DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS * 1000L * STS_TOKEN_MULTIPLIER));
        assertTrue(ApplicationTokenExpires.isValid(System.currentTimeMillis() + 24000L * 1000 * STS_TOKEN_MULTIPLIER));
        assertTrue(ApplicationTokenExpires.isValid(DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS * 1000L * STS_TOKEN_MULTIPLIER));
        assertTrue(ApplicationTokenExpires.isValid(24000L * 1000 * STS_TOKEN_MULTIPLIER));
    }


    @Test
    public void testFalseValues() {
        assertFalse(ApplicationTokenExpires.isValid(System.currentTimeMillis() + System.currentTimeMillis()));
        assertFalse(ApplicationTokenExpires.isValid(System.currentTimeMillis() - 1));
    }
}
