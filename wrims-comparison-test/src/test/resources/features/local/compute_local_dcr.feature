Feature: Run CalLite 4.1 comparison test file

  Scenario: Copy local directory, run script
    Given I copied the project named "calsim3-dcr-9.6.0" from local directory "J:\WRIMS\comparison_test_data\SmokeTest_DCR_base_RMA\SmokeTest_DCR_base_RMA"
    Then I execute the local project compute using config file "cs3_adjusted_dev_short.launch.config"

  Scenario: Copy local directory, run script, check results
    Given I copied the project named "9.3.1_danube_adj" from local directory "<DEV_NEEDS_TO_FILL_HERE>"
    And I want to compare the new result file "DSS\output\DCR2023_WRIMS_TEST.dss" to the baseline results in "DSS\output\DCR2023_DV_9.3.1_v2a_Danube_Adj_v1.8.dss".
    And I want to compare the path "/CALSIM/C_CAA003/CHANNEL//1MON/L2020A/" in both files after running
    And I want to compare the path "/CALSIM/S_OROVL/STORAGE//1MON/L2020A/" in both files after running
    And I want to compare the path "/CALSIM/S_SHSTA/STORAGE//1MON/L2020A/" in both files after running
    When I execute the local project compute using config file "cs3_adjusted_dev.launch.config"
    Then I compare the results datasets, and allow for 0.01 percent difference in each timestep