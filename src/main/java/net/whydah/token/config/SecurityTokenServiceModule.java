package net.whydah.token.config;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.token.user.DummyUserAuthenticator;
import net.whydah.token.user.UserAuthenticator;
import net.whydah.token.user.UserAuthenticatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class SecurityTokenServiceModule extends AbstractModule {
    private final static Logger log = LoggerFactory.getLogger(SecurityTokenServiceModule.class);

    private final AppConfig appConfig;
    private final String applicationmode;

    public SecurityTokenServiceModule(AppConfig appConfig, String applicationmode) {
        this.appConfig = appConfig;
        this.applicationmode = applicationmode;
    }

    @Override
    protected void configure() {
        bind(AppConfig.class).toInstance(appConfig);
        if (applicationmode.equals(ApplicationMode.DEV)) {
            log.info("Using TestUserAuthenticator to handle usercredentials");
            bind(UserAuthenticator.class).to(DummyUserAuthenticator.class);
        } else {
            bind(UserAuthenticator.class).to(UserAuthenticatorImpl.class);
            String useradminservice = appConfig.getProperty("useradminservice");
            log.info("Try to connect to useradminservice on url {}", useradminservice);
            URI useradminserviceUri = URI.create(useradminservice);
            bind(URI.class).annotatedWith(Names.named("useradminservice")).toInstance(useradminserviceUri);
        }
    }

}
