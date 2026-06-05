package gov.ca.water.wrims.engine.core.tools;

import gov.ca.water.wrims.engine.core.components.ControlData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


final class MemoryParamParserTest {

	@BeforeAll
	static void setup() {
		System.setProperty(ControlData.XSS_GLOBAL_PARAM_KEY, "512k");
		System.setProperty(ControlData.XMX_GLOBAL_PARAM_KEY, "8192M");
	}

	@AfterAll
	static void teardown() {
		System.clearProperty(ControlData.XSS_GLOBAL_PARAM_KEY);
		System.clearProperty(ControlData.XMX_GLOBAL_PARAM_KEY);
	}

	@Test
	void testStackMemParse()
	{
		String xssLocal = "gov.ca.water.wrims.engine.core.tools.memory.minStackMem";
		System.setProperty(xssLocal, "128k");

		String memory = MemoryParamParser.getStackMemory(xssLocal, "128k");

		assertEquals("-Xss128k", memory);

		System.clearProperty(xssLocal);
	}

	@Test
	void testMaxMemParse() {
		String xmxLocal = "gov.ca.water.wrims.engine.core.tools.memory.maxMem";
		System.setProperty(xmxLocal, "1024M");

		String memory = MemoryParamParser.getMaxMemory(xmxLocal, "128k");

		assertEquals("-Xmx1024M", memory);

		System.clearProperty(xmxLocal);
	}

	@Test
	void testStackMemParseGlobal() {
		String memory = MemoryParamParser.getStackMemory(null, null);

		assertEquals("-Xss512k", memory);
	}

	@Test
	void testMaxMemParseGlobal() {
		String memory = MemoryParamParser.getMaxMemory(null, null);

		assertEquals("-Xmx8192M", memory);
	}
}
