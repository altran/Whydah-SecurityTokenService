package net.whydah.token.application;

import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.config.ApplicationMode;
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
        ApplicationCredential cred = new ApplicationCredential("1212","testapp","dummy");
        ApplicationToken imp = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(cred));
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }

    @Test
    public void testCreateApplicationCredential2() {
        ApplicationCredential cred = new ApplicationCredential("1212","testapp","dummy");
        ApplicationToken imp = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(cred));
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }

    @Test
    public void testCreateApplicationToken() {
        ApplicationCredential cred = new ApplicationCredential("1212","testapp","dummy");
        ApplicationToken imp = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(cred));
        AuthenticatedApplicationTokenRepository.addApplicationToken(imp);

        ApplicationToken imp2 = AuthenticatedApplicationTokenRepository.getApplicationToken(imp.getApplicationTokenId());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp2.getApplicationID());
        assertTrue(imp2.getApplicationTokenId().length() > 12);
    }
}
