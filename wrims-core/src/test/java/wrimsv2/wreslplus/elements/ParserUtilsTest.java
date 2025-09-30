package wrimsv2.wreslplus.elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import wrimsv2.wreslparser.elements.LogUtils;

import static org.junit.jupiter.api.Assertions.*;

final class ParserUtilsTest {
    @TempDir
    Path tempDir;

    @Test()
    void parsesMultipleSequencesAndModels() throws IOException {
        // define the testing data and the expected output
        String content = """
                sequence TestSequence1 {
                    model   TestModel1
                    order   1
                }
                sequence TestSequence2 {
                    model   TestModel2
                    order   2
                }
                model TestModel1 {
                	include 'foo.wresl'
                	define TestSV {value 3.14}
                	define TestDV {
                	    std
                	    kind 'KindExample'
                	    units 'who knows?'
                	}
                }
                model TestModel2 {
                	include 'bar.wresl'
                }
                """;
        List<String> expectedModels = List.of("TestModel1", "TestModel2");
        List<String> expectedSequences = List.of("TestSequence1", "TestSequence2");
        // set up a temporary file so the parser can read from disk
        Path tempFile = tempDir.resolve("testParseWreslMain.wresl");
        Files.writeString(tempFile, content);  // puts the content string into the temp file
        String oldRunDir = GlobalData.runDir;
        try {
            GlobalData.runDir = tempFile.toAbsolutePath().getParent().toString();  // required for initialization
            Path logFile = tempDir.resolve("testParseWreslMain.log");  // required for initialization
            Files.createFile(logFile);
            LogUtils.setLogFile(logFile.toString());
            // Parse the file
            StudyTemp studyTemp = assertDoesNotThrow(() -> ParserUtils.parseWreslMain(tempFile.toString()));
            // checks
            assertNotNull(studyTemp, "Parser returned null StudyTemp");
            assertEquals(expectedModels, studyTemp.modelList, "Parsed models should match expected order");
            assertEquals(expectedSequences, studyTemp.seqList, "Parsed sequences should match expected order");
            SvarTemp svar = studyTemp.modelMap.get("TestModel1").svMap.get("TestSV");
            DvarTemp dvar = studyTemp.modelMap.get("TestModel1").dvMap.get("TestDV");
            assertEquals("3.14", svar.caseExpression.getFirst().trim(), "Parsed SV value expression should be `3.14`");
            assertEquals("KindExample", dvar.kind, "Parsed DV `kind` should be `KindExample`");
        } finally {
            // reset the global state so other tests don't conflict, note that we can't reset the log file
            GlobalData.runDir = oldRunDir;
            LogUtils.closeLogFile();
        }
    }
}
