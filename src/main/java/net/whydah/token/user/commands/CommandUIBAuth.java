package net.whydah.token.user.commands;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class CommandUIBAuth extends HystrixCommand<String> {

    private final String name;

    public CommandUIBAuth(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("UIBUserGroup"));
        this.name = name;
    }

    @Override
    protected String run() {
        return "Hello " + name + "!";
    }
}

