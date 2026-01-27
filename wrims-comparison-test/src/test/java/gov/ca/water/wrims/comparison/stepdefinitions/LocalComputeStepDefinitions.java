package gov.ca.water.wrims.comparison.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import gov.ca.water.wrims.comparison.utils.ComputeTestUtils;
import gov.ca.water.wrims.comparison.utils.LocalFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalComputeStepDefinitions {

    Path localProjectPath;
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalComputeStepDefinitions.class);

    @Given("I copied the project named {string} from local directory {string}")
    public void iCopiedTheProjectNamedFromLocalDirectory(String projectFolderName, String projectParentDir) throws IOException {
        // Resolve download target under this module's build directory
        Path sourcePath = Paths.get(projectParentDir).resolve(projectFolderName);
        localProjectPath = Paths.get("build", "testProjects", projectFolderName);
        Files.createDirectories(localProjectPath);
        Path targetFile = localProjectPath.resolve(projectFolderName);
        LOGGER.atDebug().setMessage("copying: {} -> {}").addArgument(sourcePath).addArgument(localProjectPath).log();
        LocalFileUtils.copyDirectory(sourcePath, localProjectPath);

        //Check if the target file already exists
        if(targetFile.toFile().exists()) {
            LOGGER.atInfo().setMessage("Project zip file already exists locally at: {}").addArgument(targetFile.toAbsolutePath()).log();
            LOGGER.atTrace().setMessage("setting system property '{}': {}").addArgument("wrims.test.downloadedProjectZip").addArgument(targetFile).log();
            System.setProperty("wrims.test.downloadedProjectZip", targetFile.toAbsolutePath().toString());
            return;
        }
    }

    @Then("I execute the local project compute using config file {string}")
    public void iExecuteTheLocalProjectComputeUsingConfigFile(String configFileName) {
        if (localProjectPath == null || !Files.exists(localProjectPath)) {
            LOGGER.atError().setMessage("local compute target does not exist").addKeyValue("localProjectPath", localProjectPath).log();
            throw new IllegalStateException("Target project directory does not exist.");
        }
        try {
            Path configFilePath = localProjectPath.resolve(configFileName);
            //Check for local override file. If found, copy to the extracted working directory
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("configFiles/" + configFileName);
            if (resourceStream != null) {
                LOGGER.atDebug().setMessage("local configuration found, overwriting default").addKeyValue("configFileName", configFileName).log();
                Files.copy(resourceStream, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            if(!configFilePath.toFile().exists()) {
                LOGGER.atError().setMessage("config file not found").addKeyValue("configFilePath", configFilePath).log();
                throw new IllegalStateException("Config file not found at: " + configFilePath.toAbsolutePath());
            }

            int exit = ComputeTestUtils.runControllerBatch(localProjectPath, configFilePath);
            if (exit != 0 && exit != 2) {
                LOGGER.atError().setMessage("exit code indicates an error: {}").addArgument(exit).log();
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
        } catch (Exception e) {
            LOGGER.atError().setMessage("local compute failed").setCause(e).log();
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }
}
