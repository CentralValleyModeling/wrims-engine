package wrims.compare.stepdefinitions;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import wrims.compare.utils.ComputeTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AzureStepDefinitions {

    // Public container URL hosting the project archives
    private static final String AZURE_BLOB_BASE_URL = "https://cvmwrimscomputedata.blob.core.windows.net/dwr-azure-compute-data";

    String projectFileName;
    Path localProjectZipFilePath;
    Path extractedProjectDir;

    @io.cucumber.java.en.Given("^I have a downloaded the project zip file from Azure blob named \"([^\"]*)\"$")
    public void iHaveADownloadedTheProjectZipFileFromAzureBlobNamed(String projectFileName) throws Throwable {
        this.projectFileName = projectFileName;

        // Resolve download target under this module's build directory
        Path targetDir = Paths.get("build", "testProjects");
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(projectFileName);
        localProjectZipFilePath = targetFile;


        //Check if the target file already exists
        if(targetFile.toFile().exists()) {
            System.out.println("Project zip file already exists locally at: " + targetFile.toAbsolutePath());
            System.setProperty("wrims.test.downloadedProjectZip", targetFile.toAbsolutePath().toString());
            return;
        }

        // Build the blob URL for the project file
        String blobUrl = AZURE_BLOB_BASE_URL + "/projects/" + projectFileName;

        // Obtain SAS token from system properties or environment variables
        String sas = System.getProperty("wrims.azure.sas");
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
        // Normalize token to exclude leading '?'
        if (sas.startsWith("?")) sas = sas.substring(1);

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

        // Expose the path for subsequent steps via system property
        System.setProperty("wrims.test.downloadedProjectZip", targetFile.toAbsolutePath().toString());
    }

    private String loadSasFromEnvFile() {
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

    private Path systemPath(String key) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) return null;
        return Paths.get(v);
    }

    @When("I extract the zip file to a temporary directory")
    public void iExtractTheZipFileToATemporaryDirectory() {
        try {
            // Determine the zip file path: prefer field set during download, otherwise system property
            Path zipPath = localProjectZipFilePath;
            if (zipPath == null) {
                throw new IllegalStateException("Zip file path not available. Ensure the download step ran successfully.");
            }
            if (!Files.exists(zipPath)) {
                throw new IllegalStateException("Zip file not found: " + zipPath.toAbsolutePath());
            }

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

            this.extractedProjectDir = extractDir;
            System.setProperty("wrims.test.extractedProjectDir", extractDir.toAbsolutePath().toString());
            System.out.println("Extracted " + zipPath.getFileName() + " to: " + extractDir.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract zip file to temporary directory: " + e.getMessage(), e);
        }
    }

    @And("I execute the project compute using config file {string}")
    public void iExecuteTheProjectComputeUsingConfigFile(String configFileName) {

    }

    @Then("I compare the results using input file {string} and output files named {string}")
    public void iCompareTheResultsUsingInputFileAndOutputFilesNamed(String configFile, String outputFileName) {
        try {

            Path configFilePath = extractedProjectDir.resolve(configFile);

            //if a copy the file from resources/comparisonInputFiles/{configFile} to projectDir.getParent()
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("comparisonInputFiles/" + configFile);
            if (resourceStream != null) {
                Files.copy(resourceStream, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            int exit = ComputeTestUtils.runReport(extractedProjectDir, configFilePath, outputFileName);
            if (exit != 0 && exit != 2) { // historically 2 may indicate validation diffs for reports
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
            if(exit == 2) {
                throw new IllegalStateException("Report comparison found differences in output files: " + outputFileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }

    @And("I execute the project named {string} compute using config file {string}")
    public void iExecuteTheProjectNamedComputeUsingConfigFile(String projectName, String configFileName) {
        if (this.extractedProjectDir == null || !Files.exists(this.extractedProjectDir)) {
            throw new IllegalStateException("Extracted project directory not available. Run the extract step first.");
        }
        try {
            Path extractedWorkingDir = extractedProjectDir.resolve(projectName);
            Path configFilePath = extractedWorkingDir.resolve(configFileName);
            //if a copy the file from resources/comparisonInputFiles/{configFile} to projectDir.getParent()
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("configFiles/" + configFileName);
            if (resourceStream != null) {
                Files.copy(resourceStream, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            if(!configFilePath.toFile().exists()) {
                throw new IllegalStateException("Config file not found at: " + configFilePath.toAbsolutePath());
            }

            int exit = ComputeTestUtils.runControllerBatch(extractedWorkingDir, configFilePath);
            if (exit != 0 && exit != 2) { // historically 2 may indicate validation diffs for reports
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }
}
