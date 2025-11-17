package wrims.compare.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import wrims.compare.utils.AzureUtils;
import wrims.compare.utils.ComputeTestUtils;
import wrims.compare.utils.LocalFileUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AzureStepDefinitions {

    Path extractedProjectDir;






    @Then("Compare the results using input file {string} and output files named {string}")
    public void compareTheResultsUsingInputFileAndOutputFilesNamed(String configFile, String outputFileName) {
        try {

            Path configFilePath = extractedProjectDir.resolve(configFile);

            //Check for local override file. If found, copy to the extracted working directory
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

    @When("Execute the project named {string} compute using config file {string}")
    public void executeTheProjectNamedComputeUsingConfigFile(String projectName, String configFileName) {
        if (this.extractedProjectDir == null || !Files.exists(this.extractedProjectDir)) {
            throw new IllegalStateException("Extracted project directory not available. Run the extract step first.");
        }
        try {
            Path extractedWorkingDir = extractedProjectDir.resolve(projectName);
            Path configFilePath = extractedWorkingDir.resolve(configFileName);

            //Check for local override file. If found, copy to the extracted working directory
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

    @Given("Download and extract the project zip file named {string} from Azure blob store {string}")
    public void downloadAndExtractTheProjectZipFileNamedFromAzureBlobStore(String projectFileName, String azureBlobContainerUrl) throws Throwable {
        Path projectFilePath = AzureUtils.downloadAzureProject(projectFileName, azureBlobContainerUrl, null);
        extractedProjectDir = LocalFileUtils.extractZipFile(projectFilePath);
    }
}
