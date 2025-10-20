package wrimsv2.components.controllerbatch.calsim3;

import org.junit.jupiter.api.BeforeAll;
import wrimsv2.annotations.SmokeTest;
import wrimsv2.components.ControllerBatch;
import wrimsv2.exceptions.StringNotFoundException;
import wrimsv2.results.CsvResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

public class FiveYearTest {
    private static final Logger logger = Logger.getLogger(FiveYearTest.class.getName());

    static Path inputFile = Paths.get("src/test/resources/models/calsim3/model_5_year.launch.config").toAbsolutePath();
    static Path resultsFile = Paths.get("src/test/resources/models/calsim3/output/dv.csv").toAbsolutePath();
    static Path externalDir = Paths.get("src/test/resources/models/calsim3/run/external").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        logger.info("Testing " + FiveYearTest.class.getName());
        if (!Files.exists(inputFile)) {
            throw new RuntimeException("Cannot find file: " + inputFile.toString());
        }
        String[] args = {String.format("-config=%s", inputFile)};
        int exitCode = runModel(args);
        if (exitCode != 0) {
            logger.severe(String.format("Model run failed with exitCode=%d", exitCode));
            fail();
        }
    }

    static int runModel(String[] args) {
        logger.info("Running the model prior to testing");
        List<String> command = new ArrayList<>();
        // use the same version of Java as the test runner
        String javaHome = System.getProperty("java.home");
        String javaExecutable = javaHome + "/bin/java";
        command.add(javaExecutable);
        // Increase the JVM memory limits
        command.add("-Xmx4096m");
        command.add("-Xss1024K");
        // Set up the classpath to include the dependencies
        command.add("-classpath");
        command.add("wrims-core\\build\\tmp\\libs\\*;wrims-core\\build\\libs\\*;" + System.getProperty("java.class.path"));
        // Use the same library path as the test runner
        command.add("-Djava.library.path=" + System.getProperty("java.library.path"));
        // Call the class "main" method
        command.add(ControllerBatch.class.getName());
        // Add the args passed to this method
        Collections.addAll(command, args);
        logger.info(command.toString());
        // create process
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // Set the PATH environment variable so the models external DLLs can be found
        Map<String, String> env = processBuilder.environment();
        env.put("PATH", externalDir + ";" + env.get("PATH"));
        // Set up logging for the subprocess
        processBuilder.inheritIO();
        // Run the model
        int exitCode = -1;
        try {
            Process process = processBuilder.start();
            exitCode = process.waitFor();
            logger.info("Model run complete");
        } catch (IOException | InterruptedException e) {
            logger.severe("Set up failed:\n" + e.toString());
            return 2;
        }
        return exitCode;
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
            Map<String, Double> expectations = new HashMap<>();
            expectations.put("C_CAA003", 2580.66039214385);
            expectations.put("C_DMC000", 3110.46677300951);
            expectations.put("S_OROVL", 2069.70169289075);
            expectations.put("S_SHSTA", 2967.5368816689);
            for (String varName : expectations.keySet()) {
                Double expectedVal = expectations.get(varName);
                double tolerance = 0.001 * expectedVal;  // .1% of expected value
                assertEquals(expectedVal, result.getAverage(varName), tolerance, "Average not equal for " + varName);
            }
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
            Map<String, Integer> variables = new HashMap<>();
            variables.put("C_CAA003", 60);
            variables.put("C_DMC000", 60);
            variables.put("S_OROVL", 72);  // includes 1 year of init data
            variables.put("S_SHSTA", 72);  // includes 1 year of init data
            for (String varName : variables.keySet()) {
                Integer expectedVal = variables.get(varName);
                assertEquals(expectedVal, result.getValueArray(varName).length, 0,"Array size not equal for " + varName);
            }
        } catch (StringNotFoundException e) {
            fail("not all expected variables were found in the output file");
        } catch (IOException e) {
            fail("results file could not be found");
        }
    }
}
