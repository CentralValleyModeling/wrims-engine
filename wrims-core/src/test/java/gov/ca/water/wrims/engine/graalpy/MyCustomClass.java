package gov.ca.water.wrims.engine.graalpy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyCustomClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyCustomClass.class);

    public static boolean isAnInteger(Object value) {
        return (value instanceof Integer);
    }

    public void printTest() {
        LOGGER.atInfo().setMessage("Hello from MyCustomClass!").log();
    }

    public int getAbsoluteDifference(int x, int y) {
        return Math.abs(x - y);
    }

    public String getText() {
        return "JavaTest instance with custom class";
    }
}
