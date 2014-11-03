package net.whydah.token.application;

import junit.framework.TestCase;

public class ApplicationTokenTest extends TestCase {

    public void testCreateApplicationCredential() {
        ApplicationCredential cred = new ApplicationCredential();
        cred.setApplicationID("TestApp");
        cred.setApplicationSecret("dummy");
        ApplicationToken imp = new ApplicationToken(cred.toXML());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }

    public void testCreateApplicationCredential2() {
        ApplicationCredential cred = new ApplicationCredential();
        cred.setApplicationID("1243");
        cred.setApplicationSecret("dummy");
        ApplicationToken imp = new ApplicationToken(cred.toXML());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }
}

