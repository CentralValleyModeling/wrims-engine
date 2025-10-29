Feature: Run the full CalSim3 DCR 9.6.0 config bundled with zip file from Azure blob

  Scenario: Download zip, run script, and compare results
    Given I have a downloaded the project zip file from Azure blob named "calsim3-dcr-9.6.0.zip"
    When I extract the zip file to a temporary directory
    Then I execute the project named "calsim3-dcr-9.6.0" compute using config file "cs3_adjusted_dev.launch.config"
