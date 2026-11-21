package gov.ca.water.wrims.engine.core.debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gov.ca.water.wrims.engine.core.debug.ValidateStudy.Result;
import gov.ca.water.wrims.engine.core.wreslparser.elements.Diagnostic;
import gov.ca.water.wrims.engine.core.wreslparser.elements.Diagnostic.Severity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ValidateStudyTest {

    @TempDir
    Path dir;

    private Result validate(String modelBody) throws IOException {
        Path main = dir.resolve("main.wresl");
        Files.writeString(main, "sequence s1 { model m1 order 1 }\nmodel m1 {\n" + modelBody + "\n}\n");
        return ValidateStudy.validate(main.toString());
    }

    private static List<Diagnostic> of(Result r, Severity s) {
        return r.diagnostics().stream().filter(d -> d.severity() == s).toList();
    }

    @Test
    void cleanStudyIsOk() throws IOException {
        Result r = validate("define d {std kind 'FLOW' units 'CFS'}\ngoal g {d < 5}");
        assertTrue(r.ok(), r.toString());
        assertEquals(0, r.errors());
    }

    @Test
    void goalWithLessOrEqualIsAnErrorWithFileAndLine() throws IOException {
        Result r = validate("define d {std kind 'FLOW' units 'CFS'}\ngoal g {d <= 5}");
        assertFalse(r.ok());
        Diagnostic d = of(r, Severity.ERROR).get(0);
        assertTrue(d.message().contains("'<='"), d.message());
        assertTrue(d.file().endsWith("main.wresl"), d.file());
        assertEquals(4, d.line()); // wrapper adds two lines before the body
    }

    @Test
    void laggedStateVariableIsAWarningAndStaysOk() throws IOException {
        Result r = validate("define x {value 1.0}\ndefine d {std kind 'FLOW' units 'CFS'}\n"
                + "define y {value x(-1) + 2.0}\ngoal g {d < y}");
        assertTrue(r.ok(), r.toString());
        assertEquals(1, r.warnings(), r.toString());
        assertTrue(of(r, Severity.WARNING).get(0).message().contains("state variable [x]"));
    }

    @Test
    void missingFileIsReportedAsError() {
        Result r = ValidateStudy.validate(dir.resolve("nope.wresl").toString());
        assertFalse(r.ok());
        assertTrue(r.diagnostics().get(0).message().contains("File not found"));
    }

    @Test
    void resultsDoNotLeakBetweenRuns() throws IOException {
        validate("define d {std kind 'FLOW' units 'CFS'}\ngoal g {d <= 5}");
        Result r = validate("define d {std kind 'FLOW' units 'CFS'}\ngoal g {d < 5}");
        assertTrue(r.ok(), r.toString());
        assertEquals(0, r.diagnostics().size());
    }

    @Test
    void jsonEscapesQuotesBackslashesAndNewlines() {
        String bs = String.valueOf((char) 92);   // a single backslash character
        String file = "C:" + bs + "a" + bs + "b.wresl";
        String message = "bad " + (char) 34 + "x" + (char) 34 + (char) 10 + "next";
        Result r = new Result(false, 1, 0, List.of(new Diagnostic(Severity.ERROR, file, 7, message)));

        String json = ValidateStudy.toJson(r);

        assertTrue(json.contains("C:" + bs + bs + "a" + bs + bs + "b.wresl"), json);          // backslashes doubled
        assertTrue(json.contains("bad " + bs + (char) 34 + "x" + bs + (char) 34 + bs + "nnext"), json); // quotes and newline escaped
    }

    @Test
    void jsonUsesNullForMissingFileAndLine() {
        Result r = new Result(false, 1, 0, List.of(new Diagnostic(Severity.ERROR, null, -1, "boom")));
        assertTrue(ValidateStudy.toJson(r).contains("\"file\":null,\"line\":null"));
    }
}
