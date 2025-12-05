package gov.ca.water.wrims.engine.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConfigUtilsTest {

    private static CapturingAppender appender;
    private static LoggerConfig loggerConfig;

    @BeforeEach
    void setUp() {
        System.setProperty("project.dir", ""); // needs to be set for logging to work as intended
        appender = new CapturingAppender();

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        loggerConfig = config.getLoggerConfig(ConfigUtils.class.getName());

        loggerConfig.addAppender(appender, Level.DEBUG, null);

        ctx.updateLoggers();
    }

    @AfterEach
    void tearDown() {
        loggerConfig.removeAppender(appender.getName());
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.updateLoggers();
    }

    @Test
    void loggingTest() {
        Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

        logger.info("Test message");
        logger.error("Test error message");

        List<LogEvent> events = appender.getEvents();
        assertEquals(2, events.size());
        assertEquals("Test message", events.get(0).getMessage().getFormattedMessage());
        assertEquals("Test error message", events.get(1).getMessage().getFormattedMessage());
        assertEquals(Level.INFO, events.get(0).getLevel());
        assertEquals(Level.ERROR, events.get(1).getLevel());

        String[] args = new String[] {"-config=C:\\path\\to\\properties.config"};
        assertThrows(NumberFormatException.class, () -> ConfigUtils.loadArgs(args));
        List<LogEvent> argEvents = appender.getEvents();
        assertEquals(4, argEvents.size());
        assertEquals("Config file and RUN directory must be placed in the same folder!",
            argEvents.get(2).getMessage().getFormattedMessage());
        assertEquals("Loading config file:\tC:\\path\\to\\properties.config",
            argEvents.get(3).getMessage().getFormattedMessage());

        Map<String, String> variableMap = new HashMap<>();
        variableMap.put("test", "12.0.0");
        ConfigUtils.readDouble(variableMap, "test", 25.0);
        List<LogEvent> varEvents = appender.getEvents();
        assertEquals(5, varEvents.size());
        assertEquals("test:  reading config value", varEvents.get(4).getMessage().getFormattedMessage());
    }

    @Test
    void loggingOutputFileTest() throws IOException {
        Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
        String testMsg = "Test output to file";
        logger.info(testMsg);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        // "FILE" is the name of the appender defined in log4j2.xml
        FileAppender fileAppender = config.getAppender("FILE");

        assertNotNull(fileAppender);

        String fileName = fileAppender.getFileName();
        File logFile = new File(fileName);

        if (logFile.exists()) {
            List<String> lines = Files.readAllLines(logFile.toPath());
            assertFalse(lines.isEmpty());
            boolean found = lines.stream().anyMatch(line -> line.contains(testMsg));
            assertTrue(found, "Log file should contain the test message");
        }
    }

    // Custom Appender to capture events in memory
    private static class CapturingAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        CapturingAppender() {
            super("CapturingAppender", null, null, true, Property.EMPTY_ARRAY);
            this.start();
        }

        @Override
        public void append(LogEvent event) {
            // Convert to immutable to ensure the data doesn't change after the event is processed
            events.add(event.toImmutable());
        }

        public List<LogEvent> getEvents() {
            return events;
        }
    }
}
