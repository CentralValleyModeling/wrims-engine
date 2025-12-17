Feature: Run CalLite 4.1 comparison test file

  Scenario: Copy local directory, run script
    Given I copied the project named "calsim3-dcr-9.6.0" from local directory "J:\WRIMS\comparison_test_data\SmokeTest_DCR_base_RMA\SmokeTest_DCR_base_RMA"
    Then I execute the local project compute using config file "cs3_adjusted_dev_short.launch.config"