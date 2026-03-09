package gov.ca.water.wrims.engine.core.solver;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPerformanceTimer {
    private static ListAppender appender = null;

    @BeforeAll
    static void setupAll() {
        Logger logger = (Logger) LogManager.getLogger(PerformanceTimer.class);
        appender = new ListAppender(TestPerformanceTimer.class.getName());
        appender.start();
        logger.addAppender(appender);
    }

    @AfterAll
    static void teardownAll() {
        Logger logger = (Logger) LogManager.getLogger(PerformanceTimer.class);
        logger.removeAppender(appender);
    }

    @Test
    void testUniqueString() {
        String sameString = "foo";
        PerformanceTimer timerA = new PerformanceTimer(sameString);
        PerformanceTimer timerB = new PerformanceTimer(sameString);
        assertNotSame(timerA.toString(), timerB.toString(), "timers with the same operation should still be unique");
    }

    @Test
    void testLogOnStop() {
        String operation = "testLogOnStop";
        PerformanceTimer timer = new PerformanceTimer(operation);
        timer.stop();

        List<LogEvent> found = appender.getEvents()
                                       .stream()
                                       .filter(e -> e.getMessage().getFormattedMessage().contains(operation))
                                       .filter(e -> e.getMessage().getFormattedMessage().contains("event=\"stop\""))
                                       .toList();

        assertEquals(1, found.size(), "there should only be 1 stop event logged");
    }

    @Test
    void testLogStart() {
        String operation = "testReportError";
        new PerformanceTimer(operation);

        boolean found = appender.getEvents()
                                .stream()
                                .filter(e -> e.getMessage().getFormattedMessage().contains(operation))
                                .anyMatch(e -> e.getMessage().getFormattedMessage().contains("event=\"start\""));

        assertTrue(found, "timer should emit a start event on object creation");
    }

    @Test
    void testReportError() {
        String operation = "testReportError";
        PerformanceTimer timer = new PerformanceTimer(operation);
        timer.report("error");

        boolean found = appender.getEvents()
                                .stream()
                                .filter(e -> e.getMessage().getFormattedMessage().contains(operation))
                                .filter(e -> e.getMessage().getFormattedMessage().contains("event=\"error\""))
                                .allMatch(e -> e.getLevel() == Level.ERROR);

        assertTrue(found, "all `event=\"error\"` should log at the ERROR level");
    }

    @Test
    void testRepeatReport() {
        String operation = "testRepeatReport";
        PerformanceTimer timer = new PerformanceTimer(operation);
        int numReports = 10;
        for (int i = 0; i < numReports; i++) {
            timer.report();
        }
        List<LogEvent> found = appender.getEvents()
                                       .stream()
                                       .filter(e -> e.getMessage().getFormattedMessage().contains(operation))
                                       .filter(e -> e.getMessage().getFormattedMessage().contains("event=\"report\""))
                                       .toList();
        assertEquals(numReports, found.size(), "there should be " + numReports + " reports logged");
    }
}
