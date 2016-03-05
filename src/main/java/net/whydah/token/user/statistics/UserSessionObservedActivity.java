package net.whydah.token.user.statistics;

import net.whydah.token.application.AuthenticatedApplicationRepository;
import org.valuereporter.agent.activity.ObservedActivity;

public class UserSessionObservedActivity extends ObservedActivity {
    public static final String USER_SESSION_ACTIVITY = "userSession";
    private static final String USER_SESSION_ACTIVITY_DB_KEY = "userid";

    public UserSessionObservedActivity(String userid,String applicationtokenid) {
        super(USER_SESSION_ACTIVITY, System.currentTimeMillis());
        String applicationid = AuthenticatedApplicationRepository.getApplicationIdFromApplicationTokenID(applicationtokenid);
        put("userid", userid);
        put("applicationtokenid", applicationtokenid);
        put("applicationid", applicationid);
    }
}
