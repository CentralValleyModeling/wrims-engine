/*
 * Water Resource Integrated Modeling System (WRIMS) Copyright (c) 2024.
 *
 * WRIMS 2 is copyrighted by the State of California Department of Water Resources.
 * It is licensed under the Eclipse Public License, Version 1.0.
 * See Eclipse Public License for more details.
 */

package gov.ca.water.wrims.engine.core.components;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gov.ca.water.wrims.engine.core.evaluator.TimeOperation;

final class ControllerBatchTest {

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
