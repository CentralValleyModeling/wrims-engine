package wrimsv2.wreslplus.elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import wrimsv2.wreslparser.elements.LogUtils;

final class ParserUtilsTest {
    @Test()
    void testParseWreslMain() throws IOException {
        // define the testing data and the expected output
        String content = """
                sequence TestSequence {
                    model   TestModel1
                    order   1
                }
                sequence TestSequence {
                    model   TestModel2
                    order   2
                }
                model TestModel1 {
                	include 'foo.wresl'
                	define TestSV {value 3.14}
                	define TestDV {
                	    std
                	    kind 'example'
                	    units 'who knows?'
                	}
                }
                model TestModel2 {
                	include 'bar.wresl'
                }
                """;
        List<String> expected = Arrays.asList("TestModel1", "TestModel2");
        // set up a temporary file so the parser can read from disk
        Path tempFile = Files.createTempFile("tmp", ".wresl");
        Files.writeString(tempFile, content);  // puts the content string into the temp file
        // TODO: refactor the initialization so the below global objects default to useful values
        GlobalData.runDir = tempFile.toAbsolutePath().getParent().toString();  // set the run dir to the temp files dir
        Path logFile = Files.createTempFile("tmp", ".log");  // create a log file in the temp dir too
        LogUtils.setLogFile(logFile.toString());
        // Parse the file
        StudyTemp studyTemp = ParserUtils.parseWreslMain(tempFile.toString());
        // checks
        assertEquals(expected, studyTemp.modelList);
        assertEquals(2, studyTemp.modelList.size());
        assertEquals(2, studyTemp.seqList.size());
        SvarTemp svar = studyTemp.modelMap.get("TestModel1").svMap.get("TestSV");
        DvarTemp dvar = studyTemp.modelMap.get("TestModel1").dvMap.get("TestDV");
        assertEquals("3.14", svar.caseExpression.getFirst());
        assertEquals("example", dvar.kind);

    }
}
