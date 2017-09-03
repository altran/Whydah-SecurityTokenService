package net.whydah.token.user;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.HashMap;

/**
 * Generate xml
 */
public class FreemarkerProcessor {
    private static final Logger log = LoggerFactory.getLogger(FreemarkerProcessor.class);

    private final Configuration freemarkerConfig;

    public FreemarkerProcessor() {
        freemarkerConfig = new Configuration();
        freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        freemarkerConfig.setTemplateUpdateDelay(3600);
    }

    public String toXml(UserToken userToken) {
        HashMap<String, UserToken> model = new HashMap<>(1);
        model.put("it", userToken);


        try {
            Template template = freemarkerConfig.getTemplate("usertoken.ftl");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String replacement = "<DEFCON>" + userToken.getDefcon() + "</DEFCON>";
            return writer.toString().replace("<DEFCON></DEFCON>", replacement);
        } catch (Exception e) {
            log.error("toXml failed for userToken=" + userToken.toString(), e);
            return "XML conversion failed for userToken with id " + userToken.getTokenid();
        }
    }
}
