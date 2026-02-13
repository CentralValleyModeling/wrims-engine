package gov.ca.water.wrims.comparison.stepdefinitions;

import hec.heclib.dss.HecTimeSeries;
import hec.io.TimeSeriesContainer;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalComputeStepDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalComputeStepDefinitions.class);
    Path localProjectPath = null;
    Path baselineResultFile = null;
    Path alternativeResultFile = null;
    List<String> pathsToCompare = new ArrayList<>();

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
            LOGGER.atInfo().setMessage("Project zip file already exists locally at: {}").addArgument(targetFile.toAbsolutePath()).log();
            System.setProperty("wrims.test.downloadedProjectZip", targetFile.toAbsolutePath().toString());
        }
    }

    @And("I want to compare the new result file {string} to the baseline results in {string}.")
    public void setBaseAltResultFile(String altDss, String baseDss) {
        this.alternativeResultFile = this.resolveInProject(altDss, false);
        this.baselineResultFile = this.resolveInProject(baseDss, true);
        LOGGER.atInfo()
                .setMessage("base and alt files set\n\tbase={}\n\talt={}")
                .addArgument(this.baselineResultFile)
                .addArgument(this.alternativeResultFile)
                .log();
    }

    private Path resolveInProject(String other, boolean needsToExist) {
        if (localProjectPath == null) {
            LOGGER.atError().setMessage("step used without needed initialization, specify local project first").log();
            throw new IllegalStateException("localProjectPath not set");
        }
        Path target = this.localProjectPath.resolve(other);
        if (needsToExist && !target.toFile().exists()) {
            LOGGER.atError().setMessage("file not found in the project: {}").addArgument(target).log();
            throw new IllegalStateException("file was not found: " + target);
        }
        return target;
    }

    @And("I want to compare the path {string} in both files after running")
    public void addPathToCompare(String fullPath) {
        LOGGER.atInfo().setMessage("adding path to comparison list: {}").addArgument(fullPath).log();
        this.pathsToCompare.add(fullPath);
    }

    @When("I do nothing")
    public void lazy() {
        LOGGER.atInfo().setMessage("I do nothin'!").log();
    }

    @When("I execute the local project compute using config file {string}")
    public void iExecuteTheLocalProjectComputeUsingConfigFile(String configFileName) {
        if (localProjectPath == null || !Files.exists(localProjectPath)) {
            LOGGER.atError().setMessage("could not find project directory {}").addArgument(localProjectPath).log();
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
                LOGGER.atError().setMessage("could not find configuration file in {}").addArgument(configFilePath).log();
                throw new IllegalStateException("Config file not found at: " + configFilePath.toAbsolutePath());
            }

            int exit = ComputeTestUtils.runControllerBatch(localProjectPath, configFilePath);
            if (exit != 0 && exit != 2) {
                LOGGER.atError().setMessage("local model execution failed with error code {}").addArgument(exit).log();
                throw new IllegalStateException("ControllerBatch exited with code " + exit);
            }
        } catch (Exception e) {
            LOGGER.atError().setMessage("local model execution failed with exception").setCause(e).log();
            throw new RuntimeException("Failed to execute project compute: " + e.getMessage(), e);
        }
    }

    @Then("I check the results file was created")
    public void checkForAlternativeFile() {
        if (!this.alternativeResultFile.toFile().exists()) {
            LOGGER.atError().setMessage("alternative file should exist, but doesn't: {}")
                    .addArgument(this.alternativeResultFile).log();
            throw new IllegalStateException("alternative file not found: " + this.alternativeResultFile);
        }
    }

    @Then("I compare the results datasets, and allow for {float} percent difference in each timestep")
    public void compareTotalSums(float tolerancePercent) {
        float tol = tolerancePercent / 100.0f;
        Map<String, TimeSeriesContainer> baseData = readDssForPaths(this.baselineResultFile, this.pathsToCompare);
        Map<String, TimeSeriesContainer> altData = readDssForPaths(this.alternativeResultFile, this.pathsToCompare);
        List<String> badComparisons = new ArrayList<>();
        for (String path : this.pathsToCompare) {
            LOGGER.atInfo().setMessage("comparing values for {}").addArgument(path).log();
            TimeSeriesContainer base = baseData.get(path);
            TimeSeriesContainer alt = altData.get(path);
            Map<Integer, Double> altMap = getDataMap(alt);
            if (base.numberValues == 0) {
                badComparisons.add(String.format("path:%s,base:%s,alt:%s,diff:%s,percentDiff:%s", path, null, null, null, null));
            }
            for (int i = 0; i < base.numberValues; i++) {
                Double aVal = altMap.get(base.times[i]);
                Double bVal = base.values[i];
                if (aVal != null) {  // we aren't checking for date alignment here, don't report nulls
                    Double diff = aVal - bVal;
                    double percentDiff = diff / bVal;
                    if (Math.abs(percentDiff) > tol) {
                        badComparisons.add(String.format("path:%s,base:%.3f,alt:%.3f,diff:%.3f,percentDiff:%f", path, bVal, aVal, diff, percentDiff));
                    }
                }
            }
        }
        if (!badComparisons.isEmpty()) {
            StringBuilder builder = new StringBuilder().append("comparisons failed\n");
            for (String comparison : badComparisons) {
                builder.append("\t").append(comparison).append("\n");
            }
            LOGGER.atError().setMessage(builder.toString()).log();
            throw new IllegalStateException("comparisons failed: "+ badComparisons.size());
        }
    }

    private Map<String, TimeSeriesContainer> readDssForPaths(Path dssFile, List<String> paths) {
        HecTimeSeries dss = new HecTimeSeries();
        dss.setDSSFileName(dssFile.toAbsolutePath().toString());
        Map<String, TimeSeriesContainer> found = new HashMap<>();
        for (String path : paths) {
            TimeSeriesContainer tsc = new TimeSeriesContainer();
            tsc.fullName = path;
            dss.read(tsc, true);
            found.put(path, tsc);
        }
        return found;
    }

    private static Map<Integer, Double> getDataMap(TimeSeriesContainer tsc) {
        Map<Integer, Double> m = new HashMap<>(tsc.numberValues);
        for (int i = 0; i < tsc.numberValues; i++) {
            m.put(tsc.times[i], tsc.values[i]);
        }
        return m;
    }

}
