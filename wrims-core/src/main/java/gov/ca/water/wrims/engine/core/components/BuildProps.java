package gov.ca.water.wrims.engine.core.components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildProps {
    private final Properties properties;
    public static final String DEFAULT_FILE = "wrims-engine-build.properties";
    public static final String UNKNOWN_VERSION_TAG = "3+unknown";
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildProps.class);

    /**
     * Create a `BuildProps` object with the default file for WRIMS.
     */
    public BuildProps() {
        this(DEFAULT_FILE);
    }

    /**
     * Create a `BuildProps` object given a specific properties file.
     * @param propertiesFile the String path to the properties file.
     */
    public BuildProps(String propertiesFile) {
        properties = new Properties();
        try {
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream(propertiesFile);
            properties.clear();
            properties.load(stream);
        } catch (NullPointerException | IOException e) {
            LOGGER.atError()
                  .setMessage("IOException for properties file: {}")
                  .addArgument(propertiesFile)
                  .setCause(e)
                  .log();
        }
    }

    /**
     * Get the Version Number, saved with the key `version` in properties.
     * @return value of `version` in the properties.
     */
    public String getVN() {
        String vn = properties.getProperty("version");
        if (vn == null) {
            vn = UNKNOWN_VERSION_TAG;
        }
        return vn;
    }
}