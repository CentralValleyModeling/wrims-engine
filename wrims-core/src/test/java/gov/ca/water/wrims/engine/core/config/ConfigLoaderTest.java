package gov.ca.water.wrims.engine.core.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConfigLoaderTest {
	@Test
	void testLoader() {
		Properties properties = new Properties();
		properties.put("test", "test");
		ConfigLoader loader = new ConfigLoader();
		loader.load(IOUtils.toInputStream(properties.toString().replace("{", "").replace("}", ""), StandardCharsets.UTF_8));
		assertEquals(properties, loader.getProperties());
	}
}
