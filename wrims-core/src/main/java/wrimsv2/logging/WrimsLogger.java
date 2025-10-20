package wrimsv2.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public class WrimsLogger extends Logger {
    private static final Level DEFAULT_LEVEL = Level.INFO;

    protected WrimsLogger(String name) {
        super(name, null);
        setUseParentHandlers(false); // disable root handlers
        setLevel(DEFAULT_LEVEL);
    }

    /**
     * Factory method for getting a WrimsLogger instance with custom handler & formatter.
     */
    public static WrimsLogger getLogger(String name) {
        // Java’s LogManager keeps one Logger per name — retrieve or create
        Logger baseLogger = LogManager.getLogManager().getLogger(name);
        if (baseLogger instanceof WrimsLogger myLogger) {
            return myLogger;
        }
        WrimsLogger logger = new WrimsLogger(name);

        // Add a custom handler if not already present
        if (logger.getHandlers().length == 0) {
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatterWithTime());
            consoleHandler.setLevel(DEFAULT_LEVEL);
            logger.addHandler(consoleHandler);
        }

        LogManager.getLogManager().addLogger(logger);
        return logger;
    }

    private static class SimpleFormatterWithTime extends Formatter {
        private static final DateTimeFormatter TS_FORMAT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public synchronized String format(LogRecord logRecord) {
            String timestamp = LocalDateTime.now().format(TS_FORMAT);
            String level = logRecord.getLevel().getName();
            String message = formatMessage(logRecord);
            String source = (logRecord.getSourceClassName() != null)
                    ? logRecord.getSourceClassName()
                    : logRecord.getLoggerName();

            StringBuilder sb = new StringBuilder();
            sb.append(timestamp)
                    .append(" [").append(level).append("] ")
                    .append(source).append(": ")
                    .append(message).append(System.lineSeparator());

            if (logRecord.getThrown() != null) {
                StringWriter sw = new StringWriter();
                logRecord.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append(sw);
            }

            return sb.toString();
        }
    }
}
