package net.whydah.token.data.helper;

import net.whydah.token.data.user.commands.CommandUIBAuth;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.Future;

public class CircuitBreakerCommandTest {

    @Test
    public void testDummyCommand() {
        String s = new CommandUIBAuth("Bob").execute();
        Future<String> s2 = new CommandUIBAuth("Bob").queue();
        Observable<String> s3 = new CommandUIBAuth("Bob").observe();
    }
}
