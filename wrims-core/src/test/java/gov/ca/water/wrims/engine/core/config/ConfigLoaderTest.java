package gov.ca.water.wrims.engine.core.config;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigLoaderTest
{
	@Test
	void testLoader() {
		Properties properties = new Properties();
		properties.put("test", "test");
		ConfigLoader loader = new ConfigLoader();
		loader.load();
		assertEquals(properties, loader.getProperties());
	}
}
