package wrimsv2.components.controllerbatch;

import org.junit.jupiter.api.BeforeAll;
import wrimsv2.annotations.SmokeTest;
import wrimsv2.components.ControllerBatch;
import wrimsv2.exceptions.StringNotFoundException;
import wrimsv2.results.CsvResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

public class SimpleLPTest {
    private static final Logger logger = Logger.getLogger(SimpleLPTest.class.getName());

    static Path inputFile = Paths.get("src/test/resources/models/SimpleLP/model.launch.config").toAbsolutePath();
    static Path resultsFile = Paths.get("src/test/resources/models/SimpleLP/output/dv.csv").toAbsolutePath();
    static ControllerBatch controller;

    @BeforeAll
    static void setUp() {
        logger.info("Testing " + SimpleLPTest.class.getName());
        if (!Files.exists(inputFile)) {
            throw new RuntimeException("Cannot find file: " + inputFile.toString());
        }
        String[] args = {String.format("-config=%s", inputFile)};
        logger.info("Running the model prior to testing");
        controller = new ControllerBatch(args);
        logger.info("Model run complete");
    }

    @SmokeTest
    void modelCompleted() {
        logger.info("Checking to see if the model knows it's complete");
        assertTrue(controller.isRunCompleted(), "run should be completed");
    }

    @SmokeTest
    void resultsFileCreated() {
        logger.info("Checking to see if the model results exist");
        assertTrue(Files.exists(resultsFile), "the output file should exist");
    }

    @SmokeTest
    void averageValues() {
        logger.info("Checking the average values of the model");
        try {
            CsvResult result = new CsvResult(resultsFile);
            assertEquals(6.5, result.getAverage("X1"));
            assertEquals(0.5, result.getAverage("X2"));
        } catch (StringNotFoundException e) {
            fail("not all expected variables were found in the output file");
        } catch (IOException e) {
            fail("results file could not be found");
        }
    }

    @SmokeTest
    void valueArraySize() {
        logger.info("Checking to see if the model results arrays are the expected size");
        try {
            CsvResult result = new CsvResult(resultsFile);
            String[] variables = {"X1", "X2"};
            for (String varName : variables) {
                assertEquals(12, result.getValueArray(varName).length);
            }
        } catch (StringNotFoundException e) {
            fail("not all expected variables were found in the output file");
        } catch (IOException e) {
            fail("results file could not be found");
        }
    }
}
