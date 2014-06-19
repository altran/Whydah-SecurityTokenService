package net.whydah.token.data.application;

import junit.framework.TestCase;

public class ApplicationTokenTest extends TestCase {

    public void testCreateApplicationCredential() {
        ApplicationCredential cred = new ApplicationCredential();
        cred.setApplicationID("TestApp");
        cred.setApplicationPassword("dummy");
        ApplicationToken imp = new ApplicationToken(cred.toXML());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }

    public void testCreateApplicationCredential2() {
        ApplicationCredential cred = new ApplicationCredential();
        cred.setApplicationID("Whydah");
        cred.setApplicationPassword("dummy");
        ApplicationToken imp = new ApplicationToken(cred.toXML());
        //System.out.println(imp.toXML());
        assertEquals("The generated application token is wrong.", cred.getApplicationID(), imp.getApplicationID());
        assertTrue(imp.getApplicationTokenId().length() > 12);
    }
}

