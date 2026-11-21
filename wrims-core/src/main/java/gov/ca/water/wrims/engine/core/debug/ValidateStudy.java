package gov.ca.water.wrims.engine.core.debug;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.wreslparser.elements.Diagnostic;
import gov.ca.water.wrims.engine.core.wreslparser.elements.Diagnostic.Severity;
import gov.ca.water.wrims.engine.core.wreslparser.elements.LogUtils;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;

/**
 * Headless WRESL+ pre-run check that reports every error and warning as structured data.
 *
 * <p>Usage: {@code ValidateStudy <main.wresl>}. Prints one JSON object to stdout and exits with 0
 * when the study compiles, 1 when it has errors, 2 on bad usage. All other output goes to stderr.
 */
public final class ValidateStudy {

	public record Result(boolean ok, int errors, int warnings, List<Diagnostic> diagnostics) { }

	private ValidateStudy() { }

	/** Compiles the study rooted at {@code mainWreslPath} (WRESL+) and collects what the compiler reports. */
	public static Result validate(String mainWreslPath) {

		LogUtils.resetDiagnostics();
		StudyDataSet sds = null;
		String fatal = null;

		try {
			StudyUtils.useWreslPlus = true;
			String logName = "=WreslCheck_" + FilenameUtils.removeExtension(new File(mainWreslPath).getName()) + "=.log";
			sds = StudyUtils.checkStudy(mainWreslPath, logName, "", false);
		}
		catch (IOException | RuntimeException e) {
			fatal = e.getMessage() != null ? e.getMessage() : e.toString();
		}

		List<Diagnostic> all = new ArrayList<Diagnostic>(LogUtils.getDiagnostics());
		boolean hasError = all.stream().anyMatch(d -> d.severity() == Severity.ERROR);

		// A study that did not compile but reported nothing must not look clean.
		if (fatal == null && sds == null && !hasError) fatal = "Compilation aborted without a message; see the WreslCheck log.";
		if (fatal != null) all.add(new Diagnostic(Severity.ERROR, null, -1, fatal));

		int errors = (int) all.stream().filter(d -> d.severity() == Severity.ERROR).count();
		int warnings = all.size() - errors;
		return new Result(errors == 0, errors, warnings, all);
	}

	public static String toJson(Result r) {

		StringBuilder sb = new StringBuilder();
		sb.append("{\"ok\":").append(r.ok())
		  .append(",\"errors\":").append(r.errors())
		  .append(",\"warnings\":").append(r.warnings())
		  .append(",\"diagnostics\":[");

		for (int i = 0; i < r.diagnostics().size(); i++) {
			Diagnostic d = r.diagnostics().get(i);
			if (i > 0) sb.append(',');
			sb.append("{\"severity\":").append(quote(d.severity().name().toLowerCase()))
			  .append(",\"file\":").append(d.file() == null ? "null" : quote(d.file()))
			  .append(",\"line\":").append(d.line() < 0 ? "null" : String.valueOf(d.line()))
			  .append(",\"message\":").append(quote(d.message()))
			  .append('}');
		}
		return sb.append("]}").toString();
	}

	private static String quote(String s) {

		StringBuilder sb = new StringBuilder("\"");
		for (char c : s.toCharArray()) {
			switch (c) {
				case '"': sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				default:
					if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
					else sb.append(c);
			}
		}
		return sb.append('"').toString();
	}

	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Usage: ValidateStudy <path to main .wresl file>");
			System.exit(2);
		}

		// The compiler prints progress to stdout; keep stdout for the JSON result only.
		PrintStream realOut = System.out;
		System.setOut(System.err);
		Result result;
		try {
			result = validate(args[0]);
		}
		finally {
			System.setOut(realOut);
		}

		System.out.println(toJson(result));
		System.exit(result.ok() ? 0 : 1);
	}
}
