package wrims.compare.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class AzureUtils {
    public static Path downloadAzureProject(String projectFileName, String azureBlobUrl, Optional<String> sasToken) throws IOException {
        // Resolve download target under this module's build directory
        Path targetDir = Paths.get("build", "testProjects");
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(projectFileName);

        //Check if the target file already exists
        if (targetFile.toFile().exists()) {
            System.out.println("Project zip file already exists locally at: " + targetFile.toAbsolutePath());
            return targetFile;
        }

        // Build the blob URL for the project file
        String blobUrl = azureBlobUrl + projectFileName;

        //Load or retrieve the sas token
        String sas = retrieveSasToken(sasToken);

        // Use Azure Storage Blob SDK to download the file with SAS authentication
        BlobClient blobClient = new BlobClientBuilder()
                .endpoint(blobUrl)
                .sasToken(sas)
                .buildClient();

        // Overwrite if it already exists to ensure latest copy
        blobClient.downloadToFile(targetFile.toString(), true);

        // Basic verification: ensure file exists and is non-empty
        if (!Files.exists(targetFile) || Files.size(targetFile) == 0) {
            throw new IllegalStateException("Failed to download blob: " + blobUrl);
        }

        return targetFile;
    }

    private static String retrieveSasToken(Optional<String> providedSas) {
        String sas = sas= System.getProperty("wrims.azure.sas");
        // Obtain SAS token from system properties or environment variables
        if (sas == null || sas.isBlank()) sas = System.getProperty("azure.blob.sas");
        if (sas == null || sas.isBlank()) sas = System.getenv("WRIMS_AZURE_SAS");
        if (sas == null || sas.isBlank()) sas = System.getenv("AZURE_BLOB_SAS");

        // Fallback: try to read from a local .env.cucumber file if running outside Gradle
        if (sas == null || sas.isBlank()) {
            sas = loadSasFromEnvFile();
        }

        if (sas == null || sas.isBlank()) {
            throw new IllegalStateException(
                    "Azure Blob SAS token not provided. Set one of: -Dwrims.azure.sas, -Dazure.blob.sas, " +
                            "or environment variables WRIMS_AZURE_SAS / AZURE_BLOB_SAS.");
        }

        //Normalalize SAS token by removing leading '?', if present
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
                            return val;
                        }
                    }
                } catch (IOException ignored) {
                    // ignore and try next candidate
                }
            }
        }
        return null;
    }
}
