package net.whydah.sso.commands;

import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.systemtestbase.SystemTestBaseConfig;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.token.application.CommandCheckApplicationCredentialInUAS;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

public class CommandCheckApplicationCredentialInUASTest {
    private static final Logger log = getLogger(CommandCheckApplicationCredentialInUASTest.class);


    static SystemTestBaseConfig config;

    @BeforeClass
    public static void setup() throws Exception {
        config = new SystemTestBaseConfig();
    }


    @Test
    @Ignore
    public void testCommandCheckApplicationCredentialInUAS() throws Exception {
        if (config.isSystemTestEnabled()) {
            WhydahApplicationSession applicationSession = WhydahApplicationSession.getInstance(config.tokenServiceUri.toString(), config.appCredential);

            boolean isOk = new CommandCheckApplicationCredentialInUAS(config.userAdminServiceUri, applicationSession.getActiveApplicationToken(), new ApplicationCredential("4545", "2323", "3453")).execute();
            assertTrue(!isOk);
        }

    }
}