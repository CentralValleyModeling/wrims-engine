package gov.ca.water.wrims.engine.core.tools;

import gov.ca.water.wrims.engine.core.components.ControlData;

public final class MemoryParamParser {
	public static final String MEMORY_DECISION_KEY = "gov.ca.water.wrims.engine.core.memory.decision";
	private static final boolean USE_MIN_MEMORY = Boolean.parseBoolean(System.getProperty(MEMORY_DECISION_KEY, "true"));
	private static final String XMX_GLOBAL = System.getProperty(ControlData.XMX_GLOBAL_PARAM_KEY);
	private static final String XSS_GLOBAL = System.getProperty(ControlData.XSS_GLOBAL_PARAM_KEY);

	private MemoryParamParser() {
		throw new IllegalStateException("Utility class");
	}

	public static String getMaxMemory(String localMemoryKey, String defaultValue) {
		String xmxLocal = null;
		if (localMemoryKey != null && defaultValue != null) {
			xmxLocal = System.getProperty(localMemoryKey, defaultValue);
		} else if (localMemoryKey == null) {
			xmxLocal = defaultValue;
		}
		return buildMaxParam(compareMemValues(XMX_GLOBAL, xmxLocal));
	}

	public static Long getMaxMemoryInBytes(String localMemoryKey, Long defaultValue) {
		String xmxLocal = null;
		if (localMemoryKey != null && defaultValue != null) {
			xmxLocal = System.getProperty(localMemoryKey, defaultValue.toString());
		} else if (localMemoryKey == null) {
			xmxLocal = defaultValue.toString();
		}
		return parseParam(compareMemValues(XMX_GLOBAL, xmxLocal));
	}

	public static String getStackMemory(String localMemoryKey, String defaultValue) {
		String xssLocal = null;
		if (localMemoryKey != null && defaultValue != null) {
			xssLocal = System.getProperty(localMemoryKey, defaultValue);
		} else if (localMemoryKey == null) {
			xssLocal = defaultValue;
		}
		return buildStackParam(compareMemValues(XSS_GLOBAL, xssLocal));
	}

	public static long getStackMemoryInBytes(String localMemoryKey, String defaultValue) {
		String xssLocal = null;
		if (localMemoryKey != null && defaultValue != null) {
			xssLocal = System.getProperty(localMemoryKey, defaultValue);
		} else if (localMemoryKey == null) {
			xssLocal = defaultValue;
		}
		return parseParam(compareMemValues(XSS_GLOBAL, xssLocal));
	}

	private static String compareMemValues(String left, String right) {
		long leftValue = parseParam(left);
		long rightValue = parseParam(right);

		if (USE_MIN_MEMORY) {
			return (leftValue < rightValue) ? left : right;
		} else {
			return (leftValue > rightValue) ? left : right;
		}
	}

	private static long parseParam(String value) {
		long retVal;

		if (value == null || value.isEmpty()) {
			if (USE_MIN_MEMORY) {
				return Long.MAX_VALUE;
			} else {
				return 0L;
			}
		}

		if (value.startsWith("-")) {
			value = value.substring(3);
		}
		if (value.endsWith("k") || value.endsWith("K")) {
			value = value.substring(0, value.length() - 1);
			retVal = Long.parseLong(value) * 1024L;
		} else if (value.endsWith("m") || value.endsWith("M")) {
			value = value.substring(0, value.length() - 1);
			retVal = Long.parseLong(value) * 1024L * 1024L;
		} else if (value.endsWith("g") || value.endsWith("G")) {
			value = value.substring(0, value.length() - 1);
			retVal = Long.parseLong(value) * 1024L * 1024L * 1024L;
		} else {
			retVal = Long.parseLong(value);
		}
		return retVal;
	}

	private static String buildMaxParam(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		} else if (value.startsWith("-")) {
			return value;
		}
		return String.format("-Xmx%s", value);
	}

	private static String buildStackParam(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		} else if (value.startsWith("-")) {
			return value;
		}
		return String.format("-Xss%s", value);
	}
}
