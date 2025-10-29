package wrims.compare;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/azure")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "wrims.compare.stepdefinitions")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,summary")
public class RunAzureCucumberTests {
    // This class configures Cucumber to run all features in the azure directory using the glue in stepdefinitions.
    // No code is required here.
}
