package wrimsv2.components;

import org.junit.jupiter.api.BeforeAll;
import wrimsv2.annotations.SmokeTest;
import wrimsv2.exceptions.StringNotFoundException;
import wrimsv2.results.CsvResult;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;


final class ControllerBatchTest {
    private static final Logger logger = Logger.getLogger(ControllerBatchTest.class.getName());
    private static Path modelsDirectory;

    @BeforeAll
    static void setUp() {
        Path resourcesDirectory = Paths.get("src/test/resources");
        modelsDirectory = resourcesDirectory.resolve("models");
    }

    @SmokeTest
    void runModelSimpleMILP() {
        // Set up
        logger.info("Testing SimpleMILP");
        Path modelDir = modelsDirectory.resolve("testModelSimpleMILP");
        Path resultsFile = modelDir.resolve("output/dv.csv");
        String arg = String.format("-config=%s", modelDir.resolve("model.launch.config"));
        String[] args = {arg};
        // Creating a new ControllerBatch runs the model
        ControllerBatch controller = new ControllerBatch(args);
        // Assertions
        assertTrue(controller.isRunCompleted(), "run should be completed");
        assertTrue(Files.exists(resultsFile), "the output file should exist");
        try {
            CsvResult result = new CsvResult(resultsFile);
            float[] valuesX1 = result.getVariableArray("X1");
            for (int i = 0; i < valuesX1.length; i++) {
                float month = ((i % 12) + 1);  // The model sets X1 equal to the month number, as a float
                assertEquals(month, valuesX1[i]);
            }
            float[] valuesX2 = result.getVariableArray("X2");
            for (float valX2 : valuesX2) {
                assertEquals(0.5, valX2);
            }
        } catch (StringNotFoundException e) {
            fail("not all expected variables were found in the output file");
        } catch (IOException e) {
            fail("csv should be convertible to matrix");
        }
    }
}
