Feature: Run CalLite 4.1 comparison test file

  Scenario: Download zip, run script, and compare results
    Given I have a downloaded the project zip file from Azure blob named "CalLite4.1_TF.zip"
    When I extract the zip file to a temporary directory
    And I execute the project named "CalLite4.1_TF" compute using config file "Test_02.config"
    Then I compare the results using input file "callite_version_check_dss6_6.inp" and output files named "Callite_update_compare_6_6"