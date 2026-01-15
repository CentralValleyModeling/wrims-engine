package gov.ca.water.wrims.comparison.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComputeTestUtils {

    private static final Logger LOGGER = Logger.getLogger(ComputeTestUtils.class.getName());

    public static final String XMX_PARAM = "-Xmx4096m";
    public static final String XSS_PARAM = "-Xss1024K";

    // Run the System Summary Report tool similarly to the Gradle testReport task
    // Overload with defaults: workingDir = build/testProjects, inputFile = build/(inputFilePath or testProjects/callite_version_check_dss6_6.inp)
    public static int runReport() throws IOException, InterruptedException {
        Path buildDir = Paths.get("build").toAbsolutePath();
        Path workingDir = buildDir.resolve("testProjects");
        String override = System.getProperty("inputFilePath");
        Path inputFile;
        if (override != null && !override.isBlank()) {
            Path p = Paths.get(override);
            if (!p.isAbsolute()) p = buildDir.resolve(override.replaceFirst("^\\\\|^/", ""));
            inputFile = p.normalize();
        } else {
            inputFile = buildDir.resolve("testProjects").resolve("callite_version_check_dss6_6.inp");
        }
        return runReport(workingDir, inputFile, "Callite_update_compare_6_6.pdf");
    }

    // Run the System Summary Report tool mirroring wrims-comparison-test:testReport
    public static int runReport(Path workingDir, Path inputFile, String outputFileName) throws IOException, InterruptedException {
        // Ensure natives are present under <workingDir>/../lib (module build/lib)
        // Here we verify from project root since tests run from module; also set PATH and java.library.path accordingly
        verifyNatives();

        String javaExe = Paths.get(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
        String currentCp = System.getProperty("java.class.path");

        // Resolve native lib dir under module build
        Path moduleBuildLib = Paths.get("build", "lib").toAbsolutePath();

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add(XMX_PARAM);
        cmd.add(XSS_PARAM);
        cmd.add("-XX:+CreateMinidumpOnCrash");
        cmd.add("-Djava.library.path=" + moduleBuildLib.toString());
        cmd.add("-cp");
        cmd.add(currentCp);
        cmd.add("gov.ca.dwr.callite.Batch");
        cmd.add(inputFile.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);
        String pathEnv = System.getenv("PATH");
        String newPath = moduleBuildLib.toString() + (pathEnv != null ? ";" + pathEnv : "");
        pb.environment().put("PATH", newPath);

        LOGGER.info("[Report] Launching gov.ca.dwr.callite.Batch");
        LOGGER.info(() -> "[Report] Working dir: " + safePath(workingDir));
        LOGGER.info(() -> "[Report] Input file: " + safePath(inputFile));
        LOGGER.info(() -> "[Report] java.library.path: " + safePath(moduleBuildLib));

        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                LOGGER.info("[Report] " + line);
            }
        }
        long timeoutMinutes = 60;
        if (!p.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            LOGGER.severe("[Report] Report generation timed out after " + timeoutMinutes + " minutes");
            throw new IllegalStateException("Report generation timed out after " + timeoutMinutes + " minutes");
        }
        int exit = p.exitValue();
        LOGGER.info("[Report] Exit code: " + exit);

        // Mirror Gradle outputs for visibility
        Path pdf = Paths.get("build", "testReports", outputFileName).toAbsolutePath();
        Path csv = Paths.get("build", "testReports", outputFileName+"_VALIDATION_FAILURES.csv").toAbsolutePath();
        if (Files.exists(pdf)) {
            LOGGER.info(() -> "[Report] PDF output: " + safePath(pdf));
        }
        if (exit == 2 && Files.exists(csv)) {
            LOGGER.info(() -> "[Report] Validation failures CSV: " + safePath(csv));
        }
        return exit;
    }

    // Utility: Spawn a Java process to run wrimsv2.components.ControllerBatch similar to Gradle JavaExec
    public static int runControllerBatch(Path compareDir, Path configFile) throws IOException, InterruptedException {
        //Verify that the dlls files have been loaded into the expected directory
        verifyNatives();

        Path externalDir = compareDir.resolve("Run").resolve("external");
        
        if (!Files.isDirectory(externalDir)) {
            throw new IllegalStateException("Missing Run/external directory under: " + compareDir.toAbsolutePath());
        }

        String javaExe = Paths.get(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();

        String currentCp = System.getProperty("java.class.path");
        String sep = File.pathSeparator;

        boolean onCi = System.getenv("GITHUB_ACTIONS") != null || "true".equalsIgnoreCase(System.getenv("CI"));
        // Build a recursive classpath including all subdirectories under Run/external
        List<String> cpEntries = new ArrayList<>();
        // Include jars directly under external
        cpEntries.add(externalDir.toAbsolutePath().toString() + File.separator + "*");
        // Include each directory and its jars (dir and dir/*) to catch nested libs
        try (var walk = Files.walk(externalDir)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                String abs = dir.toAbsolutePath().toString();
                cpEntries.add(abs);
                cpEntries.add(abs + File.separator + "*");
            });
        }
        // Also include wrims-core build outputs so ControllerBatch is resolvable on CI
        try {
            Path coreLibsA = Paths.get("..", "wrims-core", "build", "libs").toAbsolutePath();
            Path coreClassesA = Paths.get("..", "wrims-core", "build", "classes", "java", "main").toAbsolutePath();
            Path coreLibsB = Paths.get("wrims-core", "build", "libs").toAbsolutePath();
            Path coreClassesB = Paths.get("wrims-core", "build", "classes", "java", "main").toAbsolutePath();
            // Prefer sibling module paths; fall back to project-root style if needed
            Path[] extraDirs = new Path[] { coreLibsA, coreClassesA, coreLibsB, coreClassesB };
            int added = 0;
            for (Path p : extraDirs) {
                if (p != null && Files.isDirectory(p)) {
                    String abs = p.toAbsolutePath().toString();
                    // Add dir and wildcard for jars
                    cpEntries.add(abs);
                    cpEntries.add(abs + File.separator + "*");
                    added++;
                }
            }
            if (added > 0) {
                final int addedCount = added;
                LOGGER.info(() -> "[Compute] Added wrims-core build outputs to classpath (dirs found=" + addedCount + ")");
            } else {
                LOGGER.fine("[Compute] wrims-core build outputs not found; relying on Run/external only");
            }
        } catch (Exception ignore) {
            LOGGER.fine("[Compute] Error while probing wrims-core build outputs: " + ignore.getMessage());
        }
        String baseClasspath = String.join(sep, cpEntries);
        String classpath = onCi ? baseClasspath : baseClasspath + sep + currentCp;
        LOGGER.info(() -> "[Compute] CI detected: " + onCi + "; classpath length=" + classpath.length());
        LOGGER.fine(() -> "[Compute] Included current JVM classpath: " + (!onCi));

        // Resolve absolute path to this module's build/lib that contains native DLLs (getNatives output)
        Path moduleBuildLib = Paths.get("build", "lib").toAbsolutePath();

        // Build command (omit -cp and rely on CLASSPATH env to reduce command length on Windows)
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add(XMX_PARAM);
        cmd.add(XSS_PARAM);
        cmd.add("-XX:+CreateMinidumpOnCrash");
        cmd.add("-Djava.library.path=" + joinPaths(";", externalDir.toAbsolutePath(), moduleBuildLib));
        cmd.add("gov.ca.water.wrims.engine.core.components.ControllerBatch");
        cmd.add("-config=" + configFile.toAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Working directory: set to ${buildDir}/testProjects to mirror Gradle JavaExec configuration
        pb.directory(Paths.get("build", "testProjects").toAbsolutePath().toFile());
        // Merge stderr and stdout
        pb.redirectErrorStream(true);
        // Env PATH enrichment
        String pathEnv = System.getenv("PATH");
        String newPath = joinPaths(";", externalDir.toAbsolutePath(), moduleBuildLib) + (pathEnv != null ? ";" + pathEnv : "");
        pb.environment().put("PATH", newPath);
        // Move long classpath to environment to shorten command line
        pb.environment().put("CLASSPATH", classpath);

        LOGGER.info(() -> "[Compute] Launching ControllerBatch with config: " + safePath(configFile));
        LOGGER.info(() -> "[Compute] Working dir: " + String.valueOf(pb.directory()));
        LOGGER.info(() -> "[Compute] Classpath entries from Run/external (including subdirs): " + cpEntries.size());
        for (int i = 0; i < Math.min(10, cpEntries.size()); i++) {
            final int idx = i;
            LOGGER.fine(() -> "[Compute][cp] " + cpEntries.get(idx));
        }
        LOGGER.info(() -> "[Compute] java.library.path: " + joinPaths(";", externalDir.toAbsolutePath(), moduleBuildLib));
        // Avoid logging full PATH env; log only the head we set
        LOGGER.fine(() -> "[Compute] PATH enriched with: " + joinPaths(";", externalDir.toAbsolutePath(), moduleBuildLib));
        LOGGER.fine(() -> "[Compute] CLASSPATH length: " + classpath.length());
        try {
            boolean hasJCbc = hasDll(externalDir, "jCbc") || hasDll(moduleBuildLib, "jCbc");
            LOGGER.info(() -> "[Compute] jCbc present in libs: " + hasJCbc);
        } catch (IOException ignore) {
            LOGGER.log(Level.FINE, "[Compute] Error while checking jCbc presence: " + ignore.getMessage(), ignore);
        }

        Process p = pb.start();
        // Stream output
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                LOGGER.info("[Controller] " + line);
            }
        }
        // Wait with generous timeout
        long timeoutMinutes = 120;
        if (!p.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            LOGGER.severe("[Compute] ControllerBatch timed out after " + timeoutMinutes + " minutes");
            throw new IllegalStateException("ControllerBatch timed out after " + timeoutMinutes + " minutes");
        }
        int exit = p.exitValue();
        LOGGER.info("[Compute] ControllerBatch exit code: " + exit);
        return exit;
    }


    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private static String joinPaths(String delimiter, Path... paths) {
        StringBuilder sb = new StringBuilder();
        for (Path p : paths) {
            if (p == null) continue;
            String s = p.toString();
            if (s == null || s.isBlank()) continue;
            if (sb.length() > 0) sb.append(delimiter);
            sb.append(p.toString());
        }
        return sb.toString();
    }

    private static boolean hasDll(Path dir, String baseName) throws IOException {
        if (dir == null) return false;
        if (!Files.isDirectory(dir)) return false;
        String bnLower = baseName.toLowerCase(Locale.ROOT);
        try (var stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.equals(bnLower + ".dll") || name.startsWith(bnLower) && name.endsWith(".dll"));
        }
    }

    // Verify that <workingDir>/build/lib exists and has at least one .dll file
    public static void verifyNatives(Path workingDir) {
        if (workingDir == null) {
            throw new IllegalArgumentException("workingDir must not be null");
        }
        Path libDir = workingDir.resolve("build").resolve("lib").toAbsolutePath();
        if (!Files.isDirectory(libDir)) {
            throw new IllegalStateException("Expected natives directory does not exist: " + safePath(libDir));
        }
        try (var stream = Files.list(libDir)) {
            boolean hasDll = hasDll(libDir, "");
            if (!hasDll) {
                throw new IllegalStateException("No .dll files found under: " + safePath(libDir));
            }
            LOGGER.fine(() -> "[Compute] verifyNatives: found native DLLs under " + safePath(libDir));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to inspect natives directory: " + safePath(libDir) + ": " + e.getMessage(), e);
            throw new IllegalStateException("Failed to inspect natives directory: " + e.getMessage(), e);
        }
    }

    // Convenience overload using current working directory
    public static void verifyNatives() {
        verifyNatives(Paths.get("").toAbsolutePath());
    }

    private static String safePath(Path p) {
        try {
            return p == null ? "null" : p.toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            return String.valueOf(p);
        }
    }
}
