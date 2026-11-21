package gov.ca.water.wrims.engine.core.wreslplus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gov.ca.water.wrims.engine.core.wreslparser.elements.LogUtils;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyParser;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;
import gov.ca.water.wrims.engine.core.wreslplus.elements.GlobalData;
import gov.ca.water.wrims.engine.core.wreslplus.elements.ModelTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.ParserUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Compile-time checks for two WRIMS run-time rules the WRESL+ parser used to accept silently. */
final class WreslPlusCompileChecksTest {

    @TempDir
    Path dir;

    @BeforeEach
    void resetParserState() {
        LogUtils.setLogFile(dir.toString(), "wresl-test.log");
        GlobalData.runDir = dir.toString();
        StudyParser.total_errors = 0;
        StudyParser.error_summary = new ArrayList<>();
    }

    @AfterEach
    void closeLog() {
        LogUtils.closeLogFile(); // release the file so the temp dir can be deleted
    }

    private ModelTemp parse(String wresl) throws IOException {
        Path f = dir.resolve("test.wresl");
        Files.writeString(f, wresl);
        return ParserUtils.parseWreslFile(f.toString());
    }

    @Test
    void goalWithLessOrEqualIsReportedAsError() throws IOException {
        parse("define d {std kind 'FLOW' units 'CFS'}\ngoal g1 { d <= 5 }\n");
        assertEquals(1, StudyParser.total_errors);
        assertTrue(StudyParser.error_summary.get(0).contains("'<='"));
    }

    @Test
    void goalWithGreaterOrEqualIsReportedAsError() throws IOException {
        parse("define d {std kind 'FLOW' units 'CFS'}\ngoal g1 { d >= 5 }\n");
        assertEquals(1, StudyParser.total_errors);
    }

    @Test
    void goalWithSupportedOperatorsIsAccepted() throws IOException {
        parse("define d {std kind 'FLOW' units 'CFS'}\ngoal g1 { d < 5 }\ngoal g2 { d > 1 }\ngoal g3 { d = 2 }\n");
        assertEquals(0, StudyParser.total_errors);
    }

    @Test
    void laggedReferenceIsRecordedWithLineNumber() throws IOException {
        ModelTemp m = parse("define x {value 1.0}\ndefine y {value x(-1) + 2.0}\n");
        assertNotNull(m);
        assertEquals(2, m.laggedRefs.get("x"));
    }

    @Test
    void nonLaggedReferenceIsNotRecorded() throws IOException {
        ModelTemp m = parse("define x {value 1.0}\ndefine y {value x + 2.0}\n");
        assertNotNull(m);
        assertFalse(m.laggedRefs.containsKey("x"));
    }

    /** Compiles a one-model study and returns the compile log text. */
    private String compileStudyLog(String modelBody) throws IOException {
        Path main = dir.resolve("main.wresl");
        Files.writeString(main, "sequence s1 { model m1 order 1 }\nmodel m1 {\n" + modelBody + "\n}\n");
        StudyUtils.useWreslPlus = true;
        StudyUtils.checkStudy(main.toString(), "study-check.log", "", false);
        LogUtils.closeLogFile();
        return Files.readString(dir.resolve("study-check.log"));
    }

    @Test
    void laggedStateVariableGivesStudyWarning() throws IOException {
        String log = compileStudyLog("define x {value 1.0}\n"
                + "define d {std kind 'FLOW' units 'CFS'}\n"
                + "define y {value x(-1) + 2.0}\n"
                + "goal g {d < y}");
        assertTrue(log.contains("Earlier values of state variable [x]"), log);
        assertEquals(1, log.split("Earlier values of state variable", -1).length - 1, log); // reported once
    }

    @Test
    void laggedAliasGivesNoStudyWarning() throws IOException {
        String log = compileStudyLog("define d {std kind 'FLOW' units 'CFS'}\n"
                + "define x {alias d + 1.0 kind 'FLOW' units 'CFS'}\n"
                + "define y {value x(-1) + 2.0}\n"
                + "goal g {d < y}");
        assertFalse(log.contains("Earlier values of state variable"), log);
    }
}
