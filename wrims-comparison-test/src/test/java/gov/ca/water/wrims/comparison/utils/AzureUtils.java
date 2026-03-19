package gov.ca.water.wrims.comparison.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AzureUtils {

    private static final Logger LOGGER = Logger.getLogger(AzureUtils.class.getName());

    public static Path downloadAzureProject(String projectFileName, String azureBlobUrl, Optional<String> sasToken) throws IOException {
        // Resolve download target under this module's build directory
        Path targetDir = Paths.get("build", "testProjects");
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(projectFileName);
        LOGGER.fine(() -> "Target download directory: " + safePath(targetDir));
        LOGGER.fine(() -> "Resolved target file path: " + safePath(targetFile));

        //Check if the target file already exists
        if (targetFile.toFile().exists()) {
            LOGGER.info(() -> "Project zip file already exists locally at: " + safePath(targetFile));
            return targetFile;
        }

        // Build the blob URL for the project file
        String blobUrl = (azureBlobUrl == null ? "" : azureBlobUrl) + projectFileName;
        String safeBlobUrl = sanitizeUrl(blobUrl);
        LOGGER.info(() -> "Preparing to download from Azure Blob. blobUrl=" + safeBlobUrl);

        //Load or retrieve the sas token
        String sas = retrieveSasToken(sasToken);
        // DO NOT log the SAS token contents. Optionally log where it came from at FINE level within retrieveSasToken.

        try {
            // Use Azure Storage Blob SDK to download the file with SAS authentication
            BlobClient blobClient = new BlobClientBuilder()
                    .endpoint(blobUrl)
                    .sasToken(sas)
                    .buildClient();

            LOGGER.info(() -> "Starting Azure blob download to: " + safePath(targetFile));

            // Overwrite if it already exists to ensure latest copy
            blobClient.downloadToFile(targetFile.toString(), true);

            // Basic verification: ensure file exists and is non-empty
            long size = Files.exists(targetFile) ? Files.size(targetFile) : 0L;
            if (size <= 0L) {
                LOGGER.severe(() -> "Downloaded file is missing or empty for URL: " + safeBlobUrl);
                throw new IllegalStateException("Failed to download blob: " + safeBlobUrl);
            }
            LOGGER.info(() -> "Azure blob download complete. Bytes received=" + size + ", file=" + safePath(targetFile));
            return targetFile;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Azure blob download failed for URL: " + safeBlobUrl + ": " + e.getMessage(), e);
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Azure blob download failed: " + e.getMessage(), e);
        }
    }

    private static String retrieveSasToken(Optional<String> providedSas) {
        // If caller provided an Optional token and it's non-empty, use it.
        if (providedSas != null && providedSas.isPresent() && providedSas.get() != null && !providedSas.get().isBlank()) {
            LOGGER.info("Using SAS token provided via method parameter (value redacted).");
            return normalizeSas(providedSas.get());
        }

        // Prioritize .env.cucumber first to make local project configuration take precedence
        String sas = loadSasFromEnvFile();
        if (sas != null && !sas.isBlank()) {
            LOGGER.info("Using SAS token from .env.cucumber file (value redacted).");
            return normalizeSas(sas);
        }

        // Next, system properties (explicit JVM overrides)
        sas = System.getProperty("wrims.azure.sas");
        if (sas != null && !sas.isBlank()) {
            LOGGER.info("Using SAS token from system property 'wrims.azure.sas' (value redacted).");
            return normalizeSas(sas);
        }
        sas = System.getProperty("azure.blob.sas");
        if (sas != null && !sas.isBlank()) {
            LOGGER.info("Using SAS token from system property 'azure.blob.sas' (value redacted).");
            return normalizeSas(sas);
        }

        // Finally, environment variables
        sas = System.getenv("WRIMS_AZURE_SAS");
        if (sas != null && !sas.isBlank()) {
            LOGGER.info("Using SAS token from env 'WRIMS_AZURE_SAS' (value redacted).");
            return normalizeSas(sas);
        }
        sas = System.getenv("AZURE_BLOB_SAS");
        if (sas != null && !sas.isBlank()) {
            LOGGER.info("Using SAS token from env 'AZURE_BLOB_SAS' (value redacted).");
            return normalizeSas(sas);
        }

        LOGGER.severe("Azure Blob SAS token not provided via parameters, .env.cucumber, system properties, or environment.");
        throw new IllegalStateException(
                "Azure Blob SAS token not provided. Set one of: .env.cucumber (AZURE_BLOB_SAS or WRIMS_AZURE_SAS), " +
                        "-Dwrims.azure.sas, -Dazure.blob.sas, or environment variables WRIMS_AZURE_SAS / AZURE_BLOB_SAS.");
    }

    private static String normalizeSas(String sas) {
        if (sas == null) return null;
        sas = sas.trim();
        // Normalize SAS token by removing leading '?', if present
        if (sas.startsWith("?")) sas = sas.substring(1);
        return sas;
    }

    private static Path systemPath(String key) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) return null;
        return Paths.get(v);
    }

    private static String loadSasFromEnvFile() {
        // Try common locations for the env file
        Path[] candidates = new Path[] {
                Paths.get(".env.cucumber"),
                Paths.get("wrims-comparison-test", ".env.cucumber"),
                // fallback to projectDir if provided by Gradle/IDE Run Configuration
                systemPath("org.gradle.projectDir") != null ? systemPath("org.gradle.projectDir").resolve(".env.cucumber") : null
        };
        for (Path p : candidates) {
            if (p == null) continue;
            if (Files.exists(p)) {
                try {
                    LOGGER.fine(() -> "Attempting to load SAS token from env file: " + safePath(p));
                    List<String> lines = Files.readAllLines(p);
                    for (String line : lines) {
                        if (line == null) continue;
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx <= 0) continue;
                        String key = line.substring(0, idx).trim();
                        String val = line.substring(idx + 1).trim();
                        val = val.replaceAll("^[\"']|[\"']$", "");
                        if ("WRIMS_AZURE_SAS".equalsIgnoreCase(key) || "AZURE_BLOB_SAS".equalsIgnoreCase(key)) {
                            LOGGER.fine("Found SAS token entry in env file (value redacted).");
                            return val;
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.FINE, "Failed reading env file for SAS at " + safePath(p) + ": " + ex.getMessage(), ex);
                    // ignore and try next candidate
                }
            } else {
                LOGGER.fine(() -> "Env file not found at: " + safePath(p));
            }
        }
        return null;
    }

    private static String sanitizeUrl(String url) {
        if (url == null) return null;
        int q = url.indexOf('?');
        if (q >= 0) {
            return url.substring(0, q) + "?<redacted>";
        }
        return url;
    }

    private static String safePath(Path p) {
        try {
            return p == null ? "null" : p.toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            return String.valueOf(p);
        }
    }
}
