package net.whydah.token.data.helper;

import com.sun.jersey.api.view.Viewable;

import javax.ws.rs.core.Response;

/**
 * Created by totto on 3/30/14.
 */
public class DevModeHelper {

    public static Response return_DEV_MODE_ExampleUserToken(int n){
        return Response.ok(new Viewable("/dev_usertoken.1.ftl")).build();
    }

    public static Response return_DEV_MODE_ExampleApplicationToken(int n){
        return Response.ok(new Viewable("/dev_applicationtoken.1.ftl")).build();
    }

}
