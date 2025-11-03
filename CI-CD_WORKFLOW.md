# Continuous Integration / Continuous Deployment (CI/CD) Workflow

A Continuous Integration / Continuous Deployment (CI/CD) pipeline was established in GitHub that automates building, testing, and packaging, ensuring consistent and reliable build.

- [**wrims-engine-dependencies**](https://github.com/CentralValleyModeling/wrims-engine-dependencies): This is a private repository collecting 3rd party dependencies used by `wrims-engine` and `wrims-gui`.
- [**wrims-engine**](https://github.com/CentralValleyModeling/wrims-engine): This is a copy of the [`wrims` repository](https://github.com/CentralValleyModeling/wrims) (WRIMS 2.2) containing only the modules for the WRIMS computational engine.
- [**wrims-gui**](https://github.com/CentralValleyModeling/wrims-gui): This is a copy of the [`wrims` repository](https://github.com/CentralValleyModeling/wrims) (WRIMS 2.2) containing only the modules for the WRIMS graphical user interface (GUI).

<img width="1052" height="768" alt="GitHub WorkFlow" src="https://github.com/user-attachments/assets/4c3c8382-2918-449e-9d63-bc19fbb89f19" />

## Planning

During the planning phase, we gather requirements and coordinate on features, issues, and improvements. We use GitHub Discussions to record designs and decisions, and GitHub issues and projects to capture and track progress.

## Development

The development phase begins with work being assigned and tracked through GitHub issues. A developer then creates a new branch and opens a pull request before startig development in their local copy of the branch. As work progresses, changes are committed and pushed to the remote repository.

## Code Review

The code review phase is an iterative process in which pull requests undergo both peer review and automated checks with SonarQube Cloud and Dependabot. Feedback from peer reviewers may require revisions, after which the work re-enters the review cycle. Once the changes pass both peer and automated reviews, the pull request is merged into the main branch.

## Quality Assurance

Quality Assurance serves as the final acceptannce step, using both manual testing and automated comparison testing to verify that the system behaves as expected. If issues are identified, the work returns to the development phase for revision. As part of this process, tests document test cases, procedures, and results, strengthening regression testing and long-term quality.

## Release

In the Release phase, stable builds of `wrims-engine` and `wrims-gui` are packaged and published through GitHub. Using GitHub Releases, we tag versions, attach compiled artifacts or installers, and make them available for users and stakeholders. This ensures that only tested and approved versions are distributed, providing a clear history of official releases.
