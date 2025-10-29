package wrims.compare.utils;

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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ComputeTestUtils {

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

        System.out.println("[Report] Launching gov.ca.dwr.callite.Batch");
        System.out.println("[Report] Working dir: " + workingDir.toAbsolutePath());
        System.out.println("[Report] Input file: " + inputFile.toAbsolutePath());
        System.out.println("[Report] java.library.path: " + moduleBuildLib);

        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println("[Report] " + line);
            }
        }
        long timeoutMinutes = 60;
        if (!p.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            throw new IllegalStateException("Report generation timed out after " + timeoutMinutes + " minutes");
        }
        int exit = p.exitValue();
        System.out.println("[Report] Exit code: " + exit);

        // Mirror Gradle outputs for visibility
        Path pdf = Paths.get("build", "testReports", outputFileName).toAbsolutePath();
        Path csv = Paths.get("build", "testReports", outputFileName+"_VALIDATION_FAILURES.csv").toAbsolutePath();
        if (Files.exists(pdf)) {
            System.out.println("[Report] PDF output: " + pdf);
        }
        if (exit == 2 && Files.exists(csv)) {
            System.out.println("[Report] Validation failures CSV: " + csv);
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
        String classpath = String.join(sep, cpEntries) + sep + currentCp;

        // Resolve absolute path to this module's build/lib that contains native DLLs (getNatives output)
        Path moduleBuildLib = Paths.get("build", "lib").toAbsolutePath();

        // Build command
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add(XMX_PARAM);
        cmd.add(XSS_PARAM);
        cmd.add("-XX:+CreateMinidumpOnCrash");
        cmd.add("-Djava.library.path=" + joinPaths(";", externalDir.toAbsolutePath(), moduleBuildLib));
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add("wrimsv2.components.ControllerBatch");
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

        System.out.println("[Compute] Launching ControllerBatch with config: " + configFile.toAbsolutePath());
        System.out.println("[Compute] Working dir: " + pb.directory());
        System.out.println("[Compute] Classpath entries from Run/external (including subdirs): " + cpEntries.size());
        for (int i = 0; i < Math.min(10, cpEntries.size()); i++) {
            System.out.println("[Compute][cp] " + cpEntries.get(i));
        }
        System.out.println("[Compute] java.library.path: " + joinPaths(";", externalDir.toAbsolutePath(), moduleBuildLib));
        System.out.println("[Compute] PATH head: " + joinPaths(";", externalDir.toAbsolutePath(), moduleBuildLib));
        try {
            boolean hasJCbc = hasDll(externalDir, "jCbc") || hasDll(moduleBuildLib, "jCbc");
            System.out.println("[Compute] jCbc present in libs: " + hasJCbc);
        } catch (IOException ignore) {
        }

        Process p = pb.start();
        // Stream output
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println("[Controller] " + line);
            }
        }
        // Wait with generous timeout
        long timeoutMinutes = 120;
        if (!p.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            throw new IllegalStateException("ControllerBatch timed out after " + timeoutMinutes + " minutes");
        }
        int exit = p.exitValue();
        System.out.println("[Compute] ControllerBatch exit code: " + exit);
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
            throw new IllegalStateException("Expected natives directory does not exist: " + libDir);
        }
        try (var stream = Files.list(libDir)) {
            boolean hasDll = stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.endsWith(".dll"));
            if (!hasDll) {
                throw new IllegalStateException("No .dll files found under: " + libDir);
            }
            System.out.println("[Compute] verifyNatives: found native DLLs under " + libDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inspect natives directory: " + libDir + ": " + e.getMessage(), e);
        }
    }

    // Convenience overload using current working directory
    public static void verifyNatives() {
        verifyNatives(Paths.get("").toAbsolutePath());
    }
}
