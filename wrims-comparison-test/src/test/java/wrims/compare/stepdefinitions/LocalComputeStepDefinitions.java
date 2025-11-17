package wrims.compare.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import wrims.compare.utils.ComputeTestUtils;
import wrims.compare.utils.LocalFileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalComputeStepDefinitions {

    Path localProjectPath;

    @Given("I copied the project named {string} from local directory {string}")
    public void iCopiedTheProjectNamedFromLocalDirectory(String projectFolderName, String projectParentDir) throws IOException {
        // Resolve download target under this module's build directory
        Path sourcePath = Paths.get(projectParentDir).resolve(projectFolderName);
        localProjectPath = Paths.get("build", "testProjects", projectFolderName);
        Files.createDirectories(localProjectPath);
        Path targetFile = localProjectPath.resolve(projectFolderName);
        LocalFileUtils.copyDirectory(sourcePath, localProjectPath);

        //Check if the target file already exists
        if(targetFile.toFile().exists()) {
            System.out.println("Project zip file already exists locally at: " + targetFile.toAbsolutePath());
            System.setProperty("wrims.test.downloadedProjectZip", targetFile.toAbsolutePath().toString());
            return;
        }
    }

    @Then("I execute the local project compute using config file {string}")
    public void iExecuteTheLocalProjectComputeUsingConfigFile(String configFileName) {
        if (localProjectPath == null || !Files.exists(localProjectPath)) {
            throw new IllegalStateException("Target project directory does not exist.");
        }
        try {
            Path configFilePath = localProjectPath.resolve(configFileName);
            //Check for local override file. If found, copy to the extracted working directory
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("configFiles/" + configFileName);
            if (resourceStream != null) {
                Files.copy(resourceStream, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            if(!configFilePath.toFile().exists()) {
                throw new IllegalStateException("Config file not found at: " + configFilePath.toAbsolutePath());
            }

            int exit = ComputeTestUtils.runControllerBatch(localProjectPath, configFilePath);
            if (exit != 0 && exit != 2) {
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }
}
