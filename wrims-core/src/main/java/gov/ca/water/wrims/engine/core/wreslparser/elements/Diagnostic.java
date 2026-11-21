package gov.ca.water.wrims.engine.core.wreslparser.elements;

/**
 * One compile message in machine-readable form.
 *
 * @param severity whether the message stops the study from compiling
 * @param file     the WRESL file the message refers to, or null when it is not tied to a file
 * @param line     1-based line in {@code file}, or -1 when unknown
 * @param message  the message text
 */
public record Diagnostic(Severity severity, String file, int line, String message) {

	public enum Severity { ERROR, WARNING }
}
