package gov.ca.water.wrims.comparison.utils.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LocalFileUtils {
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static Path extractZipFile(Path zipPath) throws IOException {
        try {
            if (zipPath == null) {
                throw new IllegalStateException("Zip file path is null.");
            }
            if (!Files.exists(zipPath)) {
                throw new IllegalStateException("Zip file not found: " + zipPath.toAbsolutePath());
            }

            //Get the project file name from the zipPath
            String projectFileName = zipPath.getFileName().toString();

            // Prepare an extraction root under build/testProjects/tmp
            Path buildRoot = Paths.get("build", "testProjects", "tmp");
            Files.createDirectories(buildRoot);

            // Name extraction directory using project name (without .zip) plus timestamp to avoid collisions
            String baseName = projectFileName != null ? projectFileName : zipPath.getFileName().toString();
            if (baseName.toLowerCase().endsWith(".zip")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            String unique = baseName + "_" + Long.toString(System.currentTimeMillis());
            Path extractDir = buildRoot.resolve(unique);
            Files.createDirectories(extractDir);

            // Extract
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Path outPath = extractDir.resolve(entry.getName()).normalize();

                    // Zip Slip protection: ensure the path is within the extractDir
                    if (!outPath.startsWith(extractDir)) {
                        throw new IOException("Blocked zip entry with illegal path: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                        continue;
                    }

                    // Ensure parent directory exists
                    Files.createDirectories(outPath.getParent());

                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            // Basic verification: directory exists and is non-empty
            boolean nonEmpty = Files.exists(extractDir) && Files.list(extractDir).findAny().isPresent();
            if (!nonEmpty) {
                throw new IllegalStateException("Extraction produced an empty directory: " + extractDir.toAbsolutePath());
            }

            System.out.println("Extracted " + zipPath.getFileName() + " to: " + extractDir.toAbsolutePath());
            return extractDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract zip file to temporary directory: " + e.getMessage(), e);
        }
    }
}
