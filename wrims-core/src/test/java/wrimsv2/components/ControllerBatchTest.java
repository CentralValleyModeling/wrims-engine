package wrimsv2.components;

import org.junit.jupiter.api.BeforeAll;
import wrimsv2.annotations.SmokeTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    static String[][] readCsvToMatrix(Path sourceFile) throws IOException {
        return readCsvToMatrix(sourceFile.toAbsolutePath().toString());
    }

    static String[][] readCsvToMatrix(String sourceFile) throws IOException {
        List<String[]> rowList = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(sourceFile));
        String line;
        while ((line = br.readLine()) != null) {
            String[] lineItems = line.split(",");
            rowList.add(lineItems);
        }
        br.close();

        String[][] matrix = new String[rowList.size()][];
        for (int i = 0; i < rowList.size(); i++) {
            String[] row = rowList.get(i);
            matrix[i] = row;
        }
        return matrix;
    }

    static String[] getValueColumnFromMatrix(String[][] data, String columnName) throws NoSuchElementException {
        int columnIndex = -1;
        for (int i = 0; i < data[0].length; i++){
            if (Objects.equals(data[0][i], columnName)) {
                columnIndex = i;
                break;
            }
        }
        if (columnIndex == -1) {
            throw new NoSuchElementException("Couldn't find column: " + columnName);
        }
        return getColumnFromMatrix(Arrays.copyOfRange(data, 1, data.length), columnIndex);
    }

    static String[] getColumnFromMatrix(String[][] data, int columnIndex) {
        if (data == null || data.length == 0 || columnIndex < 0) {
            return new String[0]; // Return an empty array for invalid input
        }
        int numRows = data.length;
        String[] column = new String[numRows];
        for (int i = 0; i < numRows; i++) {
            if (data[i] != null && columnIndex < data[i].length) {
                column[i] = data[i][columnIndex];
            } else {
                column[i] = null;
            }
        }
        return column;
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
            String[][] matrix = readCsvToMatrix(resultsFile);
            String[] valueStr = getValueColumnFromMatrix(matrix, "Value");
            String[] varStr = getValueColumnFromMatrix(matrix, "Variable");
            for (int i = 0; i < valueStr.length; i++) {
                if (Objects.equals(varStr[i], "X1")) {
                    String iStr = ((i % 12) + 1) + ".0";  // The model sets X1 equal to the month number, as a float
                    assertEquals(iStr, valueStr[i]);
                } else if (Objects.equals(varStr[i], "X2")) {
                    assertEquals("0.5", valueStr[i]); // The model sets X2 to 0.5 always
                }
            }
        } catch (IOException e) {
            fail("csv should be convertible to matrix");
        }
    }
}
