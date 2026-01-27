package gov.ca.water.wrims.comparison.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class LocalFileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileUtils.class);

    public static void copyDirectory(Path source, Path target) throws IOException {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target paths must not be null.");
        }
        LOGGER.atInfo().setMessage(() -> "Copying directory from " + safePath(source) + " to " + safePath(target)).log();
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir);
                    LOGGER.atTrace().setMessage(() -> "Created directory: " + safePath(targetDir)).log();
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path dest = target.resolve(source.relativize(file));
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.atTrace().setMessage(() -> "Copied file: " + safePath(file) + " -> " + safePath(dest)).log();
                return FileVisitResult.CONTINUE;
            }
        });
        LOGGER.atInfo().setMessage(() -> "Completed copy from " + safePath(source) + " to " + safePath(target)).log();
    }

    public static Path extractZipFile(Path zipPath) throws IOException {
        try {
            if (zipPath == null) {
                throw new IllegalStateException("Zip file path is null.");
            }
            if (!Files.exists(zipPath)) {
                throw new IllegalStateException("Zip file not found: " + safePath(zipPath));
            }

            String projectFileName = zipPath.getFileName().toString();
            LOGGER.atInfo().setMessage(() -> "Starting extraction of zip: " + projectFileName + " from " + safePath(zipPath)).log();

            // Prepare an extraction root under build/testProjects/tmp
            Path buildRoot = Paths.get("build", "testProjects", "tmp");
            Files.createDirectories(buildRoot);
            LOGGER.atTrace().setMessage(() -> "Ensured build root exists: " + safePath(buildRoot)).log();

            // Name extraction directory using project name (without .zip) plus timestamp to avoid collisions
            String baseName = projectFileName != null ? projectFileName : zipPath.getFileName().toString();
            if (baseName.toLowerCase().endsWith(".zip")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            String unique = baseName + "_" + Long.toString(System.currentTimeMillis());
            Path extractDir = buildRoot.resolve(unique);
            Files.createDirectories(extractDir);
            LOGGER.atInfo().setMessage(() -> "Extracting into: " + safePath(extractDir)).log();

            // Extract
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Path outPath = extractDir.resolve(entry.getName()).normalize();

                    // Zip Slip protection: ensure the path is within the extractDir
                    if (!outPath.startsWith(extractDir)) {
                        String name = entry.getName();
                        LOGGER.atError().setMessage(() -> "Blocked zip entry with illegal path: " + name).log();
                        throw new IOException("Blocked zip entry with illegal path: " + name);
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                        LOGGER.atTrace().setMessage(() -> "Created directory from zip: " + safePath(outPath)).log();
                        continue;
                    }

                    // Ensure parent directory exists
                    Files.createDirectories(outPath.getParent());

                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.atTrace().setMessage(() -> "Extracted file: " + entry.getName() + " -> " + safePath(outPath)).log();
                    }
                }
            }

            // Basic verification: directory exists and is non-empty
            boolean nonEmpty = Files.exists(extractDir) && Files.list(extractDir).findAny().isPresent();
            if (!nonEmpty) {
                throw new IllegalStateException("Extraction produced an empty directory: " + safePath(extractDir));
            }

            LOGGER.atInfo().setMessage(() -> "Extracted " + projectFileName + " to: " + safePath(extractDir)).log();
            return extractDir;
        } catch (Exception e) {
            LOGGER.atError().setMessage("Failed to extract zip file to temporary directory: " + (zipPath == null ? "<null>" : safePath(zipPath)) + ": " + e.getMessage()).setCause(e).log();
            throw new RuntimeException("Failed to extract zip file to temporary directory: " + e.getMessage(), e);
        }
    }

    private static String safePath(Path p) {
        try {
            return p == null ? "null" : p.toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            return String.valueOf(p);
        }
    }
}
