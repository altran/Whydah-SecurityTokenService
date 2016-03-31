package net.whydah.sso.commands.adminapi.user;

import com.github.kevinsawicki.http.HttpRequest;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import net.whydah.sso.util.HttpSender;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;

public class CommandGetUser extends HystrixCommand<String> {
    private static final Logger log = getLogger(CommandGetUser.class);
    private URI userAdminServiceUri;
    private String myAppTokenId;
    private String adminUserTokenId;
    private String userID;


    public CommandGetUser(URI userAdminServiceUri, String myAppTokenId, String adminUserTokenId, String userID) {
        super(HystrixCommandGroupKey.Factory.asKey("UASUserAdminGroup"));
        this.userAdminServiceUri = userAdminServiceUri;
        this.myAppTokenId = myAppTokenId;
        this.adminUserTokenId = adminUserTokenId;
        this.userID = userID;
        if (userAdminServiceUri == null || myAppTokenId == null || adminUserTokenId == null || userID == null) {
            log.error("CommandGetUser initialized with null-values - will fail");
        }

    }

    @Override
    protected String run() {
        log.trace("CommandGetUser - myAppTokenId={}", myAppTokenId);
        String getUserUrl = userAdminServiceUri.toString() + myAppTokenId + "/" + adminUserTokenId + "/user/" + userID;

        HttpRequest request = HttpRequest.get(getUserUrl);
        int statusCode = request.code();
        String responseBody = request.body();

        switch (statusCode) {
            case HttpSender.STATUS_OK:
                log.debug("CommandGetUser - {}", responseBody);
                return responseBody;
            default:
                log.warn("Unexpected response from UAS. Response is {} content is {}", responseBody, responseBody);

        }
        throw new RuntimeException("CommandGetUser - Operation failed");



    }

    @Override
    protected String getFallback() {
        log.warn("CommandGetUser - fallback - uri={}", userAdminServiceUri.toString());
        return null;
    }


}
