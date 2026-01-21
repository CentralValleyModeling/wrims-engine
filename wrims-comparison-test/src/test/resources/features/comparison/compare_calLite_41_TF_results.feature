Feature: Run CalLite 4.1 comparison test file

  Scenario: Download zip, run script, and compare results
    Given Project folder named "CalLite4.1_TF_1768496424666" is set as the current project
    Then Compare the results using input file "callite_version_check_dss6_6.inp" and output files named "Callite_update_compare_6_6"