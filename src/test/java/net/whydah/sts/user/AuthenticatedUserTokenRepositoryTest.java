package net.whydah.sts.user;

import net.whydah.sso.config.ApplicationMode;
import net.whydah.sts.config.AppConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticatedUserTokenRepositoryTest {

    private AppConfig appConfig;

    @BeforeClass
    public static void shared() {
        Map<String, String> envs = new HashMap<>();
        envs.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        EnvHelper.setEnv(envs);

    }

    @Before
    public void setUp() throws Exception {
        appConfig = mock(AppConfig.class);

    }

    @Test
    public void updateDefaultUserSessionExtensionTime() {
        when(appConfig.getProperty(eq("user.session.timeout"))).thenReturn("240000");
        long extensionSeconds = AuthenticatedUserTokenRepository.updateDefaultUserSessionExtensionTime(appConfig);
        assertEquals(extensionSeconds, 240000L);
    }
}