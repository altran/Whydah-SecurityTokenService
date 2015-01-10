package net.whydah.token.application;

import java.util.HashMap;
import java.util.Map;


public class AuthenticatedApplicationRepository {
    private static final Map<String, ApplicationToken> apptokens = new HashMap<String, ApplicationToken>();


    public static void addApplicationToken(ApplicationToken token) {
        apptokens.put(token.getApplicationTokenId(), token);
    }

    public static ApplicationToken getApplicationToken(String applicationtokenid) {
        return apptokens.get(applicationtokenid);
    }


    public static boolean verifyApplicationToken(ApplicationToken token) {
        return token.equals(apptokens.get(token.getApplicationTokenId()));
    }

    public static boolean verifyApplicationTokenId(String applicationtokenid) {
        return apptokens.get(applicationtokenid) != null;
    }

    public static boolean verifyApplicationToken(String s) {
        try {
            //TODO baardl: Implement ApplicationTokenVerification
            String appid = s.substring(s.indexOf("<applicationtoken>") + "<applicationtoken>".length(), s.indexOf("</applicationtoken>"));
            return apptokens.get(appid) != null;
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    public static String getApplicationIdFromApplicationTokenID(String applicationtokenid) {
        ApplicationToken at = apptokens.get(applicationtokenid);
        return at.getApplicationID();
    }

}
