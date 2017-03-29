package net.whydah.token.user;

import net.whydah.sso.config.ApplicationMode;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class PinPaddingTest {

    @BeforeClass
    public static void shared() {
        Map<String, String> envs = new HashMap<>();
        envs.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        EnvHelper.setEnv(envs);

    }


    @Test
    public void testPadding() throws Exception {
        assertTrue(ActivePinRepository.paddPin("5657").length() == 4);
        assertTrue(ActivePinRepository.paddPin("657").length() == 4);
        assertTrue(ActivePinRepository.paddPin("0657").length() == 4);
        assertTrue(ActivePinRepository.paddPin("57").length() == 4);
        assertTrue(ActivePinRepository.paddPin("057").length() == 4);
        assertTrue(ActivePinRepository.paddPin("7").length() == 4);
    }
}
