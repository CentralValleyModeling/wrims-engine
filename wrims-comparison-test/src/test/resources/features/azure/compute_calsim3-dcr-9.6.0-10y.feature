Feature: Run the full CalSim3 DCR 9.6.0 config bundled with zip file from Azure blob

  Scenario: Download zip, run script, and compare results
    Given Download and extract the project zip file named "calsim3-dcr-9.6.0.zip" from Azure blob store "https://cvmwrimscomputedata.blob.core.windows.net/dwr-azure-compute-data/projects/"
    Then Execute the project named "calsim3-dcr-9.6.0" compute using config file "cs3_adjusted_dev_10y.launch.config"