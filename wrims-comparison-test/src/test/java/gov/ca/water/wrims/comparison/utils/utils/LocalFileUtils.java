package gov.ca.water.wrims.comparison.utils.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalFileUtils {

    private static final Logger LOGGER = Logger.getLogger(LocalFileUtils.class.getName());

    public static void copyDirectory(Path source, Path target) throws IOException {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target paths must not be null.");
        }
        LOGGER.info(() -> "Copying directory from " + safePath(source) + " to " + safePath(target));
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir);
                    LOGGER.fine(() -> "Created directory: " + safePath(targetDir));
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path dest = target.resolve(source.relativize(file));
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.fine(() -> "Copied file: " + safePath(file) + " -> " + safePath(dest));
                return FileVisitResult.CONTINUE;
            }
        });
        LOGGER.info(() -> "Completed copy from " + safePath(source) + " to " + safePath(target));
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
            LOGGER.info(() -> "Starting extraction of zip: " + projectFileName + " from " + safePath(zipPath));

            // Prepare an extraction root under build/testProjects/tmp
            Path buildRoot = Paths.get("build", "testProjects", "tmp");
            Files.createDirectories(buildRoot);
            LOGGER.fine(() -> "Ensured build root exists: " + safePath(buildRoot));

            // Name extraction directory using project name (without .zip) plus timestamp to avoid collisions
            String baseName = projectFileName != null ? projectFileName : zipPath.getFileName().toString();
            if (baseName.toLowerCase().endsWith(".zip")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            String unique = baseName + "_" + Long.toString(System.currentTimeMillis());
            Path extractDir = buildRoot.resolve(unique);
            Files.createDirectories(extractDir);
            LOGGER.info(() -> "Extracting into: " + safePath(extractDir));

            // Extract
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Path outPath = extractDir.resolve(entry.getName()).normalize();

                    // Zip Slip protection: ensure the path is within the extractDir
                    if (!outPath.startsWith(extractDir)) {
                        String name = entry.getName();
                        LOGGER.severe(() -> "Blocked zip entry with illegal path: " + name);
                        throw new IOException("Blocked zip entry with illegal path: " + name);
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                        LOGGER.fine(() -> "Created directory from zip: " + safePath(outPath));
                        continue;
                    }

                    // Ensure parent directory exists
                    Files.createDirectories(outPath.getParent());

                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.fine(() -> "Extracted file: " + entry.getName() + " -> " + safePath(outPath));
                    }
                }
            }

            // Basic verification: directory exists and is non-empty
            boolean nonEmpty = Files.exists(extractDir) && Files.list(extractDir).findAny().isPresent();
            if (!nonEmpty) {
                throw new IllegalStateException("Extraction produced an empty directory: " + safePath(extractDir));
            }

            LOGGER.info(() -> "Extracted " + projectFileName + " to: " + safePath(extractDir));
            return extractDir;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to extract zip file to temporary directory: " + (zipPath == null ? "<null>" : safePath(zipPath)) + ": " + e.getMessage(), e);
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
