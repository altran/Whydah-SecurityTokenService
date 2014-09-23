package net.whydah.token;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.ApplicationMode;
import net.whydah.token.config.SecurityTokenServiceModule;
import net.whydah.token.data.user.ActiveUserTokenRepository;
import net.whydah.token.resource.UserTokenResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ServiceStarter {
    private final static Logger logger = LoggerFactory.getLogger(ServiceStarter.class);
    private HttpServer httpServer;
    private int webappPort;
    private final String contextpath="/tokenservice";

    public static void main(String[] args) throws IOException {
        ServiceStarter serviceStarter = new ServiceStarter();
        serviceStarter.startServer();
        try {
            // wait forever...
            Thread.currentThread().join();
        } catch (InterruptedException ie) {
        }
        serviceStarter.stop();
    }

    protected void startServer() throws IOException {
        String appMode = ApplicationMode.getApplicationMode();
        AppConfig appConfig = new AppConfig();
        Injector injector = Guice.createInjector(new SecurityTokenServiceModule(appConfig, appMode));

        logger.info("Starting grizzly...");

        ServletHandler adapter = new ServletHandler();
        adapter.setContextPath(contextpath);
        adapter.addInitParameter("com.sun.jersey.config.property.packages", "net.whydah.token");
        adapter.setProperty(ServletHandler.LOAD_ON_STARTUP, "1");

        GuiceFilter filter = new GuiceFilter();
        adapter.addFilter(filter, "guiceFilter", null);

        GuiceContainer container = new GuiceContainer(injector);
        adapter.setServletInstance(container);

        webappPort = Integer.valueOf(appConfig.getProperty("service.port"));
        httpServer = new HttpServer();
        ServerConfiguration serverconfig = httpServer.getServerConfiguration();
        serverconfig.addHttpHandler(adapter, "/");
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, webappPort);
        httpServer.addListener(listener);
        httpServer.start();

        ActiveUserTokenRepository.initializeDistributedMap();  // Kick-off hazelcast distributed tokensessionmap
        UserTokenResource.initializeDistributedMap();  // Kick-off hazelcast distributed ticketmap

        logger.info("SecurityTokenService started on port {}", webappPort);
        logger.info("IAM_MODE = {}",ApplicationMode.getApplicationMode());
        logger.info("Status: http://localhost:{}{}/",webappPort,contextpath);
        logger.info("Testpage = {}",appConfig.getProperty("testpage"));
        logger.info("TestDriverWeb:   http://localhost:{}{}/",webappPort,contextpath);
        logger.info("WADL:   http://localhost:{}{}/application.wadl",webappPort,contextpath);
        logger.info("\n");
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
