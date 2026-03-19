Feature: Run CalLite 4.1 comparison test file

  Scenario: Download zip, run script, and compare results
    Given Download and extract the project zip file named "CalLite4.1_TF.zip" from Azure blob store "https://cvmwrimscomputedata.blob.core.windows.net/dwr-azure-compute-data/projects/"
    When Execute the project named "CalLite4.1_TF" compute using config file "Test_02.config"
    Then Compare the results using input file "callite_version_check_dss6_6.inp" and output files named "Callite_update_compare_6_6"