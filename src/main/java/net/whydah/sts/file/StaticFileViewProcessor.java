package net.whydah.sts.file;

import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Returns static text files from /webfiles on the classpath.
 */
@Provider
public class StaticFileViewProcessor implements ViewProcessor<String> {
    private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());
    private static final String WEBFILES_PATH = "/webfiles";
    private static final String CONTENT_ENCODING_UTF8 = "UTF-8";

    @Override
    public String resolve(String name) {
        log.debug("Resolving path {}", name);
        if (name.endsWith(".css") || name.endsWith("js") || name.endsWith("html")) {
            return name;
        } else {
            return null;
        }
    }

    @Override
    public void writeTo(String filepath, Viewable viewable, OutputStream out) throws IOException {
        String path = WEBFILES_PATH + filepath;
        InputStream classpathStream = getClass().getResourceAsStream(path);
        if (classpathStream == null)
            throw new IllegalArgumentException("Missing path " + path + " for " + filepath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(classpathStream, CONTENT_ENCODING_UTF8));
        String line;
        while ((line = reader.readLine()) != null) {
            out.write(line.getBytes(CONTENT_ENCODING_UTF8));
            out.write('\n');
        }
        reader.close();
    }
}
