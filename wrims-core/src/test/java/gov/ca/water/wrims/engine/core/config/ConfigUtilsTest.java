package gov.ca.water.wrims.engine.core.config;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigUtilsTest {

    private static CapturingAppender appender;
    private static LoggerConfig loggerConfig;

    @BeforeEach
    void setUp() {
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

        String[] args = new String[]{"-config=C:\\path\\to\\properties.config"};
        assertThrows(NumberFormatException.class, () -> ConfigUtils.loadArgs(args));

        Map<String, String> variableMap = new HashMap<>();
        variableMap.put("test", "12.0.0");
        ConfigUtils.readDouble(variableMap, "test", 25.0);
    }

    @Test
    void loggingOutputFileTest() throws IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
        String testMsg = "Test output to file";
        logger.info(testMsg);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        FileAppender fileAppender = config.getAppender("FILE");
        assertNotNull(fileAppender);

        File logFile = new File(fileAppender.getFileName());

        // If file exists, poll for the message (accounting for async appender)
        if (logFile.exists()) {
            boolean found = false;
            long deadline = System.currentTimeMillis() + 5000; // up to 5s
            while (System.currentTimeMillis() < deadline && !found) {
                List<String> lines = Files.readAllLines(logFile.toPath());
                found = lines.stream().anyMatch(line -> line.contains(testMsg));
                if (!found) Thread.sleep(50);
            }
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
