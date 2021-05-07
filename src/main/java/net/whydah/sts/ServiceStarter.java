package net.whydah.sts;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sts.config.AppConfig;
import net.whydah.sts.config.SecurityTokenServiceModule;
import net.whydah.sts.user.AuthenticatedUserTokenRepository;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.valuereporter.client.activity.ObservedActivityDistributer;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class ServiceStarter {
    private static final Logger log = LoggerFactory.getLogger(ServiceStarter.class);
    private HttpServer httpServer;
    private int webappPort;
    private static final String CONTEXTPATH = "/tokenservice";
    public static final String IMPLEMENTATION_VERSION = ServiceStarter.class.getPackage().getImplementationVersion();

    private static KeyPair publicKeyPair;

    public static void main(String[] args) throws IOException {
        //http://www.slf4j.org/legacy.html#jul-to-slf4j
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
        setupPublicKey();
        ServiceStarter serviceStarter = new ServiceStarter();
        serviceStarter.startServer();
        try {
            // wait forever...
            Thread.currentThread().join();
        } catch (InterruptedException ie) {
            log.error("Running server killed by interrupt");
        }
        serviceStarter.stop();
    }

    private static void setupPublicKey() {
        try {
            if (publicKeyPair != null) {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");

                SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
                keyGen.initialize(1024, random);

                KeyPair pair = keyGen.generateKeyPair();
                publicKeyPair = pair;
            }
        } catch (Exception e) {
            log.error("Unable to create pgp security", e);
        }
    }

    protected void startServer() throws IOException {
        String appMode = ApplicationMode.getApplicationMode();
        AppConfig appConfig = new AppConfig();

        Injector injector = Guice.createInjector(new SecurityTokenServiceModule(appConfig, appMode));

        log.info("Starting SecurityTokenService... version:{}", IMPLEMENTATION_VERSION);


        //Start Valuereporter event distributer.
        try {
            String reporterHost = appConfig.getProperty("valuereporter.host");
            String reporterPort = appConfig.getProperty("valuereporter.port");
            String prefix = appConfig.getProperty("applicationname").replace(" ","");
            int cacheSize = Integer.parseInt(appConfig.getProperty("valuereporter.activity.batchsize"));
            int forwardInterval = Integer.parseInt(appConfig.getProperty("valuereporter.activity.postintervalms"));
            new Thread(ObservedActivityDistributer.getInstance(reporterHost, reporterPort, prefix, cacheSize, forwardInterval)).start();
            log.info("Started ObservedActivityDistributer({},{},{},{},{})",reporterHost, reporterPort, prefix, cacheSize, forwardInterval);

        } catch (Exception e) {
            log.warn("Error in valueReporter property configuration - unable to start ObservedActivityDistributer");
        }
        


        final WebappContext context = new WebappContext("grizzly", CONTEXTPATH);
        GuiceContainer container = new GuiceContainer(injector);
        final ServletRegistration servletRegistration = context.addServlet("ServletContainer", container);
        servletRegistration.addMapping("/*");
        servletRegistration.setInitParameter("com.sun.jersey.config.property.packages", "net.whydah");
        servletRegistration.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        context.addFilter("guiceFilter", GuiceFilter.class);


        try {
            webappPort = Integer.valueOf(appConfig.getProperty("service.port"));
        } catch (Exception e) {
            webappPort = 9998;
        }
        httpServer = new HttpServer();
        //ServerConfiguration serverconfig = httpServer.getServerConfiguration();
        //serverconfig.addHttpHandler(handler, "/");
        NetworkListener listener = new NetworkListener("grizzly server", NetworkListener.DEFAULT_NETWORK_HOST, webappPort);
        httpServer.addListener(listener);
        
       
        context.deploy(httpServer);
        
        httpServer.start();


        AuthenticatedUserTokenRepository.initializeDistributedMap();  // Kick-off hazelcast distributed tokensession-map

        log.info("SecurityTokenService started on port {}, IAM_MODE = {}", webappPort, ApplicationMode.getApplicationMode());
        log.info("Version: {}", IMPLEMENTATION_VERSION);
        log.info("Status: http://localhost:{}{}/", webappPort, CONTEXTPATH);
        log.info("Health: http://localhost:{}{}/health", webappPort, CONTEXTPATH);
        log.info("WADL:   http://localhost:{}{}/application.wadl", webappPort, CONTEXTPATH);
        log.info("Testpage = {}, TestDriverWeb:   http://localhost:{}{}/", appConfig.getProperty("testpage"), webappPort, CONTEXTPATH);
    }

    public int getPort() {
        return webappPort;
    }

    public static KeyPair getPublicKeyPair() {
        return publicKeyPair;
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }
}
