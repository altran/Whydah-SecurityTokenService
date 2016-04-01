package net.whydah.sso.commands.adminapi.user;


import com.github.kevinsawicki.http.HttpRequest;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import net.whydah.sso.util.HttpSender;
import org.slf4j.Logger;

import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;

public class CommandGetUserAggregate extends HystrixCommand<String> {
    private static final Logger log = getLogger(CommandGetUserAggregate.class);
    private URI userAdminServiceUri;
    private String myAppTokenId;
    private String adminUserTokenId;
    private String userID;


    public CommandGetUserAggregate(URI userAdminServiceUri, String myAppTokenId, String adminUserTokenId, String userID) {
        super(HystrixCommandGroupKey.Factory.asKey("UASUserAdminGroup"));
        this.userAdminServiceUri = userAdminServiceUri;
        this.myAppTokenId = myAppTokenId;
        this.adminUserTokenId = adminUserTokenId;
        this.userID = userID;
        if (userAdminServiceUri == null || myAppTokenId == null || adminUserTokenId == null || userID == null) {
            log.error("CommandGetUserAggregate initialized with null-values - will fail");
        }

    }

    @Override
    protected String run() {
        log.trace("CommandGetUserAggregate - myAppTokenId={}", myAppTokenId);

        String getUserAggregate = userAdminServiceUri.toString() + myAppTokenId + "/" + adminUserTokenId + "/useraggregate/" + userID;
        HttpRequest request = HttpRequest.get(getUserAggregate);
        int statusCode = request.code();
        String responseBody = request.body();

        switch (statusCode) {
            case HttpSender.STATUS_OK:
                log.trace("CommandGetUserAggregate - {}", responseBody);
                return responseBody;
            default:
                log.error("CommandGetUserAggregate - Unexpected response from UAS. Response is {} content is {}", responseBody, responseBody);
        }
        throw new RuntimeException("CommandGetUserAggregate - Operation failed");
    }

    @Override
    protected String getFallback() {
        log.warn("CommandGetUserAggregate - fallback - uri={}", userAdminServiceUri.toString());
        return null;
    }


}
