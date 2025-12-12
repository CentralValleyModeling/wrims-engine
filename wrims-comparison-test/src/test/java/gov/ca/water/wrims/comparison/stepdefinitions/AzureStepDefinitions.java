package gov.ca.water.wrims.comparison.stepdefinitions;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import gov.ca.water.wrims.comparison.utils.AzureUtils;
import gov.ca.water.wrims.comparison.utils.ComputeTestUtils;
import gov.ca.water.wrims.comparison.utils.LocalFileUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AzureStepDefinitions {

    private static final Logger LOGGER = Logger.getLogger(AzureStepDefinitions.class.getName());

    Path extractedProjectDir;

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

    /**
     * Resolve a file under a base directory and, if a matching resource exists under the given resourceFolder,
     * copy it over the resolved file path. Logs all operations for observability.
     * @param baseDir base directory to resolve against
     * @param relativeFile file name or relative path
     * @param resourceFolder classpath folder to look for an override (e.g., "configFiles" or "comparisonInputFiles")
     * @return the resolved Path to the file
     */
    private Path resolveAndApplyResourceOverride(Path baseDir, String relativeFile, String resourceFolder) {
        Path resolved = baseDir.resolve(relativeFile);
        LOGGER.fine(() -> "Resolved path for " + relativeFile + ": " + safePath(resolved));
        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceFolder + "/" + relativeFile)) {
            if (resourceStream != null) {
                Files.copy(resourceStream, resolved, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info(() -> "Loaded override file from resources (" + resourceFolder + ") into " + safePath(resolved));
            } else {
                LOGGER.fine(() -> "No override resource found under '" + resourceFolder + "' for " + relativeFile + "; using existing file if present.");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to apply resource override for " + relativeFile + " from folder '" + resourceFolder + "': " + ex.getMessage(), ex);
        }
        return resolved;
    }

    @Then("Compare the results using input file {string} and output files named {string}")
    public void compareTheResultsUsingInputFileAndOutputFilesNamed(String configFile, String outputFileName) {
        LOGGER.info(() -> "Starting report comparison. configFile=" + configFile + ", outputFileName=" + outputFileName);
        try {
            Path configFilePath = resolveAndApplyResourceOverride(extractedProjectDir, configFile, "comparisonInputFiles");
            LOGGER.fine(() -> "Resolved comparison input path: " + safePath(configFilePath));

            int exit = ComputeTestUtils.runReport(extractedProjectDir, configFilePath, outputFileName);
            LOGGER.info(() -> "Report tool exit code=" + exit + "; outputFileName=" + outputFileName);
            if (exit != 0 && exit != 2) { // historically 2 may indicate validation diffs for reports
                LOGGER.severe("Report tool failed with non-success exit code: " + exit);
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
            if(exit == 2) {
                LOGGER.warning("Report comparison found differences. Failing the step. Output files prefix: " + outputFileName);
                throw new IllegalStateException("Report comparison found differences in output files: " + outputFileName);
            }
            LOGGER.info("Report comparison completed successfully with no differences.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed during report comparison step: " + e.getMessage(), e);
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }

    @When("Execute the project named {string} compute using config file {string}")
    public void executeTheProjectNamedComputeUsingConfigFile(String projectName, String configFileName) {
        if (this.extractedProjectDir == null || !Files.exists(this.extractedProjectDir)) {
            LOGGER.severe("Extracted project directory is not available; did you run the extract step?");
            throw new IllegalStateException("Extracted project directory not available. Run the extract step first.");
        }
        LOGGER.info(() -> "Starting compute execution. projectName=" + projectName + ", configFileName=" + configFileName);
        try {
            Path extractedWorkingDir = extractedProjectDir.resolve(projectName);
            Path configFilePath = resolveAndApplyResourceOverride(extractedWorkingDir, configFileName, "configFiles");
            LOGGER.fine(() -> "Resolved workingDir=" + safePath(extractedWorkingDir) + ", configPath=" + safePath(configFilePath));

            if(!configFilePath.toFile().exists()) {
                LOGGER.severe("Config file not found at: " + safePath(configFilePath));
                throw new IllegalStateException("Config file not found at: " + configFilePath.toAbsolutePath());
            }

            int exit = ComputeTestUtils.runControllerBatch(extractedWorkingDir, configFilePath);
            LOGGER.info(() -> "ControllerBatch exit code=" + exit);
            if (exit != 0) {
                LOGGER.severe("ControllerBatch failed with non-success exit code: " + exit);
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
            LOGGER.info("ControllerBatch completed successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to execute project compute: " + e.getMessage(), e);
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }

    @Given("Download and extract the project zip file named {string} from Azure blob store {string}")
    public void downloadAndExtractTheProjectZipFileNamedFromAzureBlobStore(String projectFileName, String azureBlobContainerUrl) throws Throwable {
        String safeUrl = sanitizeUrl(azureBlobContainerUrl);
        LOGGER.info(() -> "Starting Azure download. projectFileName=" + projectFileName + ", containerUrl=" + safeUrl);
        Path projectFilePath = AzureUtils.downloadAzureProject(projectFileName, azureBlobContainerUrl, null);
        LOGGER.info(() -> "Downloaded project to: " + safePath(projectFilePath));
        extractedProjectDir = LocalFileUtils.extractZipFile(projectFilePath);
        LOGGER.info(() -> "Extracted project directory: " + safePath(extractedProjectDir));
    }
}
