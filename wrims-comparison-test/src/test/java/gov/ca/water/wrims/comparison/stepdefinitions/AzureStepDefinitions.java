package gov.ca.water.wrims.comparison.stepdefinitions;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import gov.ca.water.wrims.comparison.utils.AzureUtils;
import gov.ca.water.wrims.comparison.utils.ComputeTestUtils;
import gov.ca.water.wrims.comparison.utils.LocalFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;



public class AzureStepDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStepDefinitions.class.getName());

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
        LOGGER.atTrace().setMessage(() -> "Resolved path for " + relativeFile + ": " + safePath(resolved)).log();
        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceFolder + "/" + relativeFile)) {
            if (resourceStream != null) {
                Files.copy(resourceStream, resolved, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.atInfo().setMessage(() -> "Loaded override file from resources (" + resourceFolder + ") into " + safePath(resolved)).log();
            } else {
                LOGGER.atTrace().setMessage(() -> "No override resource found under '" + resourceFolder + "' for " + relativeFile + "; using existing file if present.").log();
            }
        } catch (Exception ex) {
            LOGGER.atWarn().setMessage("Failed to apply resource override for " + relativeFile + " from folder '" + resourceFolder + "': " + ex.getMessage()).setCause(ex).log();
        }
        return resolved;
    }

    @Then("Compare the results using input file {string} and output files named {string}")
    public void compareTheResultsUsingInputFileAndOutputFilesNamed(String configFile, String outputFileName) {
        LOGGER.atInfo().setMessage(() -> "Starting report comparison. configFile=" + configFile + ", outputFileName=" + outputFileName).log();
        try {
            Path configFilePath = resolveAndApplyResourceOverride(extractedProjectDir, configFile, "comparisonInputFiles");
            LOGGER.atTrace().setMessage(() -> "Resolved comparison input path: " + safePath(configFilePath)).log();

            int exit = ComputeTestUtils.runReport(extractedProjectDir, configFilePath, outputFileName);
            LOGGER.atInfo().setMessage(() -> "Report tool exit code=" + exit + "; outputFileName=" + outputFileName).log();
            if (exit != 0 && exit != 2) { // historically 2 may indicate validation diffs for reports
                LOGGER.atError().setMessage("Report tool failed with non-success exit code: " + exit).log();
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
            if(exit == 2) {
                LOGGER.atWarn().setMessage("Report comparison found differences. Failing the step. Output files prefix: " + outputFileName).log();
                throw new IllegalStateException("Report comparison found differences in output files: " + outputFileName);
            }
            LOGGER.atInfo().setMessage("Report comparison completed successfully with no differences.").log();
        } catch (Exception e) {
            LOGGER.atError().setMessage("Failed during report comparison step: " + e.getMessage()).setCause(e).log();
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }

    @When("Execute the project named {string} compute using config file {string}")
    public void executeTheProjectNamedComputeUsingConfigFile(String projectName, String configFileName) {
        if (this.extractedProjectDir == null || !Files.exists(this.extractedProjectDir)) {
            LOGGER.atError().setMessage("Extracted project directory is not available; did you run the extract step?").log();
            throw new IllegalStateException("Extracted project directory not available. Run the extract step first.");
        }
        LOGGER.atInfo().setMessage(() -> "Starting compute execution. projectName=" + projectName + ", configFileName=" + configFileName).log();
        try {
            Path extractedWorkingDir = extractedProjectDir.resolve(projectName);
            Path configFilePath = resolveAndApplyResourceOverride(extractedWorkingDir, configFileName, "configFiles");
            LOGGER.atTrace().setMessage(() -> "Resolved workingDir=" + safePath(extractedWorkingDir) + ", configPath=" + safePath(configFilePath)).log();

            if(!configFilePath.toFile().exists()) {
                LOGGER.atError().setMessage("Config file not found at: " + safePath(configFilePath)).log();
                throw new IllegalStateException("Config file not found at: " + configFilePath.toAbsolutePath());
            }

            int exit = ComputeTestUtils.runControllerBatch(extractedWorkingDir, configFilePath);
            LOGGER.atInfo().setMessage(() -> "ControllerBatch exit code=" + exit).log();
            if (exit != 0) {
                LOGGER.atError().setMessage("ControllerBatch failed with non-success exit code: " + exit).log();
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
            LOGGER.atInfo().setMessage("ControllerBatch completed successfully.").log();
        } catch (Exception e) {
            LOGGER.atError().setMessage("Failed to execute project compute: " + e.getMessage()).setCause(e).log();
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }

    @Given("Download and extract the project zip file named {string} from Azure blob store {string}")
    public void downloadAndExtractTheProjectZipFileNamedFromAzureBlobStore(String projectFileName, String azureBlobContainerUrl) throws Throwable {
        String safeUrl = sanitizeUrl(azureBlobContainerUrl);
        LOGGER.atInfo().setMessage(() -> "Starting Azure download. projectFileName=" + projectFileName + ", containerUrl=" + safeUrl).log();
        Path projectFilePath = AzureUtils.downloadAzureProject(projectFileName, azureBlobContainerUrl, null);
        LOGGER.atInfo().setMessage(() -> "Downloaded project to: " + safePath(projectFilePath)).log();
        extractedProjectDir = LocalFileUtils.extractZipFile(projectFilePath);
        LOGGER.atInfo().setMessage(() -> "Extracted project directory: " + safePath(extractedProjectDir)).log();
    }

    @Given("Project folder named {string} is set as the current project")
    public void projectFolderNamedIsSetAsTheCurrentProject(String projectFolderName) {
        //set the extractedProjectDir to the specified folder within the extracted directory
        Path projectPath = Paths.get("build", "testProjects", "tmp", projectFolderName);
        if (!Files.exists(projectPath)) {
            LOGGER.atError().setMessage("Extracted project directory is not available").log();
            throw new IllegalStateException("Extracted project directory not available. Run the extract step and verify the project folder name is correct.");
        }
        this.extractedProjectDir = projectPath;
    }
}
