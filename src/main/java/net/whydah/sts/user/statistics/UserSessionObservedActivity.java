package net.whydah.sts.user.statistics;

import net.whydah.sts.application.AuthenticatedApplicationTokenRepository;
import org.valuereporter.activity.ObservedActivity;

public class UserSessionObservedActivity extends ObservedActivity {
    public static final String USER_SESSION_ACTIVITY = "userSession";
    private static final String USER_SESSION_ACTIVITY_DB_KEY = "userid";

    public UserSessionObservedActivity(String userid,String usersessionfunction,String applicationtokenid) {
        super(USER_SESSION_ACTIVITY, System.currentTimeMillis());
        String applicationid = AuthenticatedApplicationTokenRepository.getApplicationIdFromApplicationTokenID(applicationtokenid);
        put("userid", userid);
        put("usersessionfunction", usersessionfunction);
        put("applicationtokenid", applicationtokenid);
        put("applicationid", applicationid);
    }
}
