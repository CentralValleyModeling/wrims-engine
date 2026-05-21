package gov.ca.water.wrims.engine.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.flogger.FluentLogger;

public final class ConfigLoader {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private Properties properties;

	public ConfigLoader() {
	}

	public void load(InputStream stream) {
		try {
			if (stream == null) {
				logger.atSevere().log("config.properties not found in classpath");
				return;
			}
			properties = new Properties();
			properties.load(stream);
		} catch (IOException ex) {
			logger.atSevere().withCause(ex).log("Failed to load config.properties");
		}
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public Properties getProperties() {
		return properties;
	}
}
