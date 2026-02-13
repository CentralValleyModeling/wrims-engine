package gov.ca.water.wrims.engine.core.components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildProps {
    private final Properties properties;
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildProps.class);

    public BuildProps() {
        properties = new Properties();
        String defaultFile = "wrims-engine-build.properties";
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(defaultFile);
        try {
            properties.load(stream);
        } catch (NullPointerException | IOException e) {
            LOGGER.atError().setMessage(System.getProperty("java.class.path")).log();
            LOGGER.atError().setMessage("could not find default properties file: {}").addArgument(defaultFile).log();
        }
    }

    public String getVN() {
        return properties.getProperty("version");
    }
}