### wrims-comparison-test README

This module contains Cucumber-based comparison and Azure download/compute tests for WRIMS. 
It orchestrates downloading example projects, running batch computes, and generating comparison reports.

---

### Location
- Module path: `wrims-engine/wrims-comparison-test`

---

### Project layout overview
- `build.gradle`
    - Declares tasks and dependencies for tests, native libs, and report generation.
    - Loads optional environment variables from a local `.env.cucumber` file.
- `src/test/resources/features/`
    - `azure/`
        - Cucumber features that download test projects from Azure Storage and run computes, e.g.:
            - `compare_calLite_41_TF.feature`
            - `compute_calsim3-dcr-9.6.0-10y.feature`
            - `compute_calsim3-dcr-9.6.0-short.feature`
    - `comparison/`
        - Features that compare outputs to reference results, e.g.:
            - `compare_calLite_41_TF_results.feature`
- `src/test/resources/comparisonInputFiles/`
    - Inputs for the comparison report tool, inp files stored here will automatically override input files 
    - stored a downloaded project zip at run time so developers can override inputs without modifying the zip
    - when running the azure compute tests. e.g.:
      - `callite_version_check_dss6_6.inp`.
- `src/test/java/gov/ca/water/wrims/comparison/`
    - `RunAzureCucumberTests.java` — JUnit Platform suite to run the Azure feature(s).
    - `stepdefinitions/` — Cucumber step definitions used by the features.
        - `AzureStepDefinitions.java` — steps for Azure download and compute workflows.
    - `utils/`
        - `AzureUtils.java` — helper for downloading blobs from Azure using a SAS token.
        - `ComputeTestUtils.java` — helper methods for launching compute/report tasks.
- Build outputs (generated at runtime):
    - `build/lib/` — unzipped native libraries required by compute.
    - `build/testProjects/` — downloaded project zip files are cached here.
    - `build/testProjects/tmp` — unzipped sample projects and generated outputs.
        - Example outputs:
            - `Callite_update_compare_6_6.pdf` (comparison report)
            - `Callite_update_compare_6_6_VALIDATION_FAILURES.csv` (only on validation failures)

---

### Key Gradle tasks
- `getNatives` (type `Sync`)
    - Unzips platform native libraries into `build/lib`.
- `getTestProjects` (type `Sync`)
    - Unzips packaged test projects into `build/testProjects`.
- `testExecute` (type `JavaExec`)
    - Runs a CalLite compute using `ControllerBatch` with a test config.
- `testReport` (type `JavaExec`)
    - Runs the comparison report tool, producing `Callite_update_compare_6_6.pdf` and optionally a `..._VALIDATION_FAILURES.csv`.
- `runAzureCucumberTests` (type `Test`)
    - Runs only the Azure Cucumber suite `RunAzureCucumberTests` on JUnit Platform.
- `test` (type `Test`)
    - Default test task; excludes the Azure suite.
- `clear-tmp-projects` (type `Delete`)
    - Deletes `build/testProjects/tmp` within this module.
    - Useful for cleaning up extracted projects without deleting cached zips.

---

### Azure feature tests require a SAS token
Azure feature tests download files from a protected Azure Blob container. You must provide a valid SAS token.

You can provide the SAS token in one of these ways (checked in this order):
1) Direct parameter to steps (rare; mostly internal)
2) Java system properties: `
    - `-Dazure.blob.sas=...`
3) Environment variables:
    - `AZURE_BLOB_SAS`
4) Local env file in this module:
    - `.env.cucumber` (recommended for local dev; git-ignored)

The loader normalizes a leading `?` if present, so both `?sv=...` and `sv=...` are accepted.

---

### Create `.env.cucumber` with your SAS token (required for Azure tests)
Create a text file named `.env.cucumber` in the module root:

- Path: `wrims-engine/wrims-comparison-test/.env.cucumber`
- This file is intentionally ignored by Git to prevent private tokens leaking into the public repo.
- Add the following key with your SAS token value:

```
AZURE_BLOB_SAS=?sv=...&ss=...&srt=...&sp=...&se=...&st=...&spr=...&sig=...
```

Notes:
- Do not quote the value unless needed; quotes are stripped if present.
- Never commit or share your SAS token.

---

### Running tests and tasks (Windows PowerShell examples)
From repository root (where `gradlew.bat` resides):

- Run only the Azure Cucumber suite:
```
./gradlew.bat :wrims-comparison-test:runAzureCucumberTests
```
- Ensure `.env.cucumber` exists or pass a token explicitly, e.g.:
```
./gradlew.bat :wrims-comparison-test:runAzureCucumberTests -Dwrims.azure.sas="sv=...&sig=..."
```

- Generate compute and comparison report locally (CalLite example):
```
./gradlew.bat :wrims-comparison-test:testReport
```
- Outputs:
    - `wrims-engine/wrims-comparison-test/build/testProjects/Callite_update_compare_6_6.pdf`
    - Optional: `..._VALIDATION_FAILURES.csv` if validations fail.

- Clear temporary extracted projects:
```
./gradlew.bat :wrims-comparison-test:clear-tmp-projects
```

---

### Troubleshooting
- Missing SAS token:
    - You will see an error like: “Azure Blob SAS token not provided. Set one of: -Dwrims.azure.sas, -Dazure.blob.sas, or environment variables WRIMS_AZURE_SAS / AZURE_BLOB_SAS.”
    - Ensure `.env.cucumber` is present or pass `-Dwrims.azure.sas=...`.
- Native libraries not found:
    - The test tasks automatically depend on `getNatives`; if running custom commands, ensure natives are in `build/lib`.
- File paths and spaces:
    - `testExecute` logs the full Java command it builds; check the Gradle output for diagnostics.

---

### Security reminder
- Treat SAS tokens as secrets. Do not post them in code reviews, logs, or commit them to the repository.
- The project avoids logging actual token contents; only sources are mentioned at fine logging levels.

---

### Maintainers
- WRIMS Engineering team. Open an issue or PR in this repository for improvements to tests or documentation.
