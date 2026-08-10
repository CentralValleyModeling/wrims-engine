package gov.ca.water.wrims.comparison;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/local")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gov.ca.water.wrims.comparison.stepdefinitions")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,summary")
public class RunLocalCucumberTest {
    // This class configures Cucumber to run all features in the local directory using the glue in stepdefinitions.
    // No code is required here.
}
