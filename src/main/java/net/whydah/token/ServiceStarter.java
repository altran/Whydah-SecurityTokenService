package net.whydah.token;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.SecurityTokenServiceModule;
import net.whydah.token.user.ActiveUserTokenRepository;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.valuereporter.agent.activity.ObservedActivityDistributer;
import org.valuereporter.agent.http.HttpObservationDistributer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class ServiceStarter {
    private static final Logger log = LoggerFactory.getLogger(ServiceStarter.class);
    private HttpServer httpServer;
    private int webappPort;
    private static final String CONTEXTPATH = "/tokenservice";
    public static final String IMPLEMENTATION_VERSION = ServiceStarter.class.getPackage().getImplementationVersion();


    public static void main(String[] args) throws IOException {
        //http://www.slf4j.org/legacy.html#jul-to-slf4j
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

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

    protected void startServer() throws IOException {
        String appMode = ApplicationMode.getApplicationMode();
        AppConfig appConfig = new AppConfig();

        Injector injector = Guice.createInjector(new SecurityTokenServiceModule(appConfig, appMode));

        log.info("Starting SecurityTokenService... version:{}", IMPLEMENTATION_VERSION);


        //Start Valuereporter event distributer.
        String reporterHost = appConfig.getProperty("valuereporter.host");
        String reporterPort = appConfig.getProperty("valuereporter.port");
        String prefix = appConfig.getProperty("applicationname");
        int cacheSize = Integer.parseInt(appConfig.getProperty("valuereporter.activity.batchsize"));
        int forwardInterval = Integer.parseInt(appConfig.getProperty("valuereporter.activity.postintervalms"));
        new Thread(new ObservedActivityDistributer(reporterHost, reporterPort, prefix, cacheSize, forwardInterval)).start();
        new Thread(new HttpObservationDistributer(reporterHost, reporterPort, prefix)).start();

        
        
     
      
        //HUYDO: this is the old implementation which will break when grizzly dependencies are updated 
        
//		  ServletHandler adapter = new ServletHandler();
//        adapter.setContextPath(CONTEXTPATH);
//        adapter.addInitParameter("com.sun.jersey.config.property.packages", "net.whydah");
//        adapter.setProperty(ServletHandler.LOAD_ON_STARTUP, "1");
//
//        
//        GuiceFilter filter = new GuiceFilter();
//        adapter.addFilter(filter, "guiceFilter", null);
//       
//        GuiceContainer container = new GuiceContainer(injector);
//        adapter.setServletInstance(container);
       
        
        //HUYDO: new implementation

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
        
        
        
        
        
        ActiveUserTokenRepository.initializeDistributedMap();  // Kick-off hazelcast distributed tokensession-map

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

    public void stop() {
        if(httpServer != null) {
            httpServer.stop();
        }
    }
}
