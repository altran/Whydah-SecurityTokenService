package net.whydah.token.application;

import net.whydah.token.config.ApplicationMode;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationTokenTest {

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
    }

    @Test
    public void testCreateApplicationCredential() {
        ApplicationCredential cred = new ApplicationCredential();
        cred.setApplicationID("1212");
        cred.setApplicationSecret("dummy");
        ApplicationToken imp = new ApplicationToken(cred.toXML());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }

    @Test
    public void testCreateApplicationCredential2() {
        ApplicationCredential cred = new ApplicationCredential();
        cred.setApplicationID("1243");
        cred.setApplicationSecret("dummy");
        ApplicationToken imp = new ApplicationToken(cred.toXML());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }

    @Test
    public void testCreateApplicationToken() {
        ApplicationCredential cred = new ApplicationCredential();
        cred.setApplicationID("2233");
        cred.setApplicationSecret("dummy");
        ApplicationToken imp = new ApplicationToken(cred.toXML());
        AuthenticatedApplicationRepository.addApplicationToken(imp);

        ApplicationToken imp2 = AuthenticatedApplicationRepository.getApplicationToken(imp.getApplicationTokenId());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp2.getApplicationID());
        assertTrue(imp2.getApplicationTokenId().length() > 12);
    }
}
