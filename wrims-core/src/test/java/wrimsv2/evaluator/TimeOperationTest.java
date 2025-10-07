package wrimsv2.evaluator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import wrimsv2.evaluator.TimeOperation;

final class TimeOperationTest {

    @CsvSource({"1Month", "1MON", "1month", "1mon", "1mOnTh", "1MoN"})
    @ParameterizedTest
    void testIsMonthlyInterval(String intervalName) {
        assertTrue(TimeOperation.isMonthlyInterval(intervalName), "Interval: " + intervalName + " should be monthly");
    }

    @CsvSource({"1DAY", "1Day", "1DaY"})
    @ParameterizedTest
    void testIsNotMonthlyInterval(String intervalName) {
        assertFalse(TimeOperation.isMonthlyInterval(intervalName), "Interval: " + intervalName + " should not be monthly");
    }
}
