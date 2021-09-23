package net.whydah.sts.health;

import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.whydah.ThreatSignal;
import net.whydah.sts.config.AppConfig;
import net.whydah.sts.user.AuthenticatedUserTokenRepository;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AsyncHealthServiceTest {

    @Test
    public void testTheatSignalRingbuffer() throws InterruptedException {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        System.setProperty(AppConfig.IAM_CONFIG_KEY, "src/test/testconfig.properties");
        AuthenticatedUserTokenRepository.getLastSeenMapSize();
        AsyncHealthService asyncHealthService = new AsyncHealthService(10, ChronoUnit.MILLIS);
        asyncHealthService.waitUntilReady(60, TimeUnit.SECONDS);
        assertTrue(asyncHealthService.isActivelyUpdatingCurrentHealth());
        System.out.printf("JSON before any theat-signals: %s%n", asyncHealthService.getHealthJson());
        asyncHealthService.addThreatSignal(createTestThreatSignal(1));
        Thread.sleep(10);
        asyncHealthService.addThreatSignal(createTestThreatSignal(2));
        Thread.sleep(1500);
        System.out.printf("JSON after 2 threat-signals: %s%n", asyncHealthService.getHealthJson());
        asyncHealthService.addThreatSignal(createTestThreatSignal(3));
        Thread.sleep(10);
        asyncHealthService.addThreatSignal(createTestThreatSignal(4));
        Thread.sleep(10);
        asyncHealthService.addThreatSignal(createTestThreatSignal(5));
        Thread.sleep(10);
        asyncHealthService.addThreatSignal(createTestThreatSignal(6));
        Thread.sleep(1500);
        System.out.printf("JSON after 6 threat-signals: %s%n", asyncHealthService.getHealthJson());
        asyncHealthService.shutdown();
        assertTrue(asyncHealthService.waitUntilShutdown(5, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertFalse(asyncHealthService.isActivelyUpdatingCurrentHealth());
    }

    private ThreatSignal createTestThreatSignal(int i) {
        return new ThreatSignal("test-source", "test-emitter", "test-instant", "test-severity", "test-text-" + i);
    }
}