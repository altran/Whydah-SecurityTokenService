package net.whydah.token.user;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.HashMap;

/**
 * Generate
 */
public class FreemarkerProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FreemarkerProcessor.class);

    private final Configuration freemarkerConfig;

    public FreemarkerProcessor() {
        freemarkerConfig = new Configuration();
        freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        freemarkerConfig.setTemplateUpdateDelay(3600);
    }

    /*
    public String toXml(UserToken2 userToken) {
        HashMap<String, UserToken2> model = new HashMap<>(1);
        model.put("it", userToken);
        try {
            Template template = freemarkerConfig.getTemplate("usertoken2.ftl");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            logger.error("toXml failed for userToken=" + userToken.toString(), e);
            return "XML conversion failed for userToken with id " + userToken.getTokenid();
        }
    }
    */

    public String toXml(UserToken2 userToken) {
        HashMap<String, UserToken2> model = new HashMap<>(1);
        model.put("it", userToken);
        try {
            Template template = freemarkerConfig.getTemplate("usertoken2.ftl");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            logger.error("toXml failed for userToken=" + userToken.toString(), e);
            return "XML conversion failed for userToken with id " + userToken.getTokenid();
        }
    }
}
