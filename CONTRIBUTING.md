# Contributing Guidelines

Thank you for considering contributing to this project!
To keep things smooth and maintain high quality, please follow the rules and best practices outlined below.

- Be respectful and constructive in code reviews and discussions.
- Follow the Code of Conduct at all times.
- All code must be reviewed and approved via a Pull Request (PR)
- Write clear, concise commit messages and PR descriptions.
- Keep the ```main``` branch production-ready. Do not push directly to it.

## Developer Workflow Overview

> [!IMPORTANT]  
> This repository is currently `private`, and has limited the permissions for contributing to the repository. If you are unsure about your permissions, ask about it the [Developer Permissions](https://github.com/CentralValleyModeling/wrims-engine/discussions/44) discussion.

1. [Assign an issue to a resource](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/creating-an-issue)
1. [Create a new branch](#branching-strategies) to address the assigned issue
1. Address the issue
1. [Commit changes](#commits)
1. [Open a pull request](#pull-requests)
1. Have a maintainer review the pull request
    - A pull request may require revisions from the reviewer before being approved.
1. Merge approved pull request into the main branch
1. Delete the working branch if no longer needed

## Coding Standards and Style Guides

### General Principles (All Languages)

- Follow the code style for the context (language, file purpose, etc)
- Write clear, readable, and self-explanatory code.
- Use consistent indentation.
- Use meaningful names for variables, functions, and classes.
- Avoid hard-coding values - use constants or configuration where appropriate.
- Comment why something is done, not what is done (code should explain the "what").
- Avoid global variables unless necessary.

### Fortran

#### File Naming
- Use ```.f90``` for modern Fortran source files.
- Lowercase file names with underscores: ```calculate_area.f90```

#### Style
- Use ```implicit none``` to avoid undeclared variables.
- Group related code into modules and subroutines.
 
#### Naming
- Use ```snake_case``` for variables and subroutines.
- Modules: ```module_name```
- Constants:```ALL_CAPS```

#### Example
```
module circle_utils
  implicit none
  real, parameter :: PI = 3.14
contains
  function calculate_area(radius) result(area)
    real, intent(in) :: radius
    real :: area
    area = PI * radius * radius
  end function calculate_area
end module circle_utils
```

### Java

#### File Naming
- Each class should go in its own ```.java``` file named after the class.

#### Style
- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

#### Naming
- Classes: ```PascalCase```
- Methods and variables: ```camelCase```
- Constants: ```UPPER_SNAKE_CASE``` (with ```static final```)

#### Example
```
public class Circle {
    public static final double PI = 3.14;

    public double calculateArea(double radius) {
        return PI * radius * radius;
    }
}
```
### Python

The short answer: [PEP 8](https://peps.python.org/pep-0008/). It's encouraged to use a code formatter like `autopep8`, or `black`. As long as it's PEP 8 compliant, it's fine.

#### Scripts

Python scripts should provide a [CLI](https://docs.python.org/3/library/argparse.html) entry point so other users don't need to change source code in order to run the script with new inputs.

Limit the use of `jupyter` notebooks for scripts that others might use for production or development activities. Notebooks are fine for documentation, tutorials, and the like.

#### Modules & Packages

Under most circumstances, Python modules shouldn't cause side effects upon `import`, they should not use `global`, and should not use `print` when `logging` is more appropriate.

Avoid the use of [namespace packages](https://docs.python.org/3/glossary.html#term-namespace-package).


## Branching Strategies

### ```main``` Branch
- Keep ```main``` always **stable and production-ready**.
- Support **collaborative development**.
- Ensure features and fixes are tested before release.
- Require PR reviews.
- Disallow direct pushes.

## Git Operations and Pull Requests Guidelines

### Branching

- All branches should have a known goal and lifecycle.
- The branch name should summarize its goal include one of these prefixes:
    - `feature/` - branches with new features
    - `bugfix/` - branches with bug fixes
    - `devops/` - branches that change the CI/CD process, and similar activities.
    - `docs/` - for branches only containing documentation addition or edits
- Branches should be lightweight according to the goal and lifecycle.
- **Main branch must remain deployable at all times.**
    - No pull request should be made to `main` with partially completed work (with very limited exceptions which need to be coordinated before opening the pull request).
    - "Deployable" means:
        - Developer tested
        - Automated tests are passing
        - Feature complete
        - Peer-reviewed and accepted
        - Static analysis completed
        - Test coverage metrics goal met
- **Other people's branches:**
  - Do not push commits to someone else's feature branch; at least not without coordinating with them first.
     - It is somewhat rude.
     - There could be local changes that the originator has not pushed up yet that you are missing.
  - Instead submit a PR to that branch if you feel you have a constructive change for it.
- **Branches in general**
    - Do not leave feature branches laying around.
- **Avoid Git Force Pushes** 
    - See [below](#use-the-force) sections below for details on when `--force` is okay.
    - When possible, avoid the use of ```git push --force```. If the option is available, use ```git push --force-with-lease``` (or the equivalent option in the application in use). ```--force``` performs a blind overwrite of the branch on the server, which may sometimes undesirably blast away changes. ```--force-with-lease``` checks the status of the branch on the server first, and if it has changed sinc the last ```fetch``` or ```pull```, will fail the ```push```.

### Commits
- The primary rule is "be clear about what this commit is doing."
- [How to Write a Git Commit Message](https://cbea.ms/git-commit/)
- If you cannot open the above guide, the format for a commit is:
    - first 60 characters, short summary
    - blank line
    - more detailed description
- If work is done for a ticket, the GitHub ID should be referenced. This allows GitHub/GitHub issues to link together and cross-reference. 
- Try to group related changes in a single commit.

#### Use the Force?
- It is okay to rewrite your local feature branch history so that it is simpler and more clear
    - UNTIL you push the branch and share it
    - Though it may still be okay, coordinate with the team.
  
### Pull Requests
- The lifecycle of a pull request is the developer's responsibility.
- As much as possible, pull requests should have small changes. However, this means as small as possible.
- Preferred is to merge in any successful change during development as soon as possible. This means a given effort may involve a series of pull requests. This is normal and preferred.
- NOTE: while prudence and fixing complexity can override the above two rules, it should not as a matter of course and only be done after smaller changes have been attempted.

#### Pull Request Title/Summary
- If PR is a draft, but you still want review/feedback/builds, prefix with one of the following for better communication with reviewers. Note that GitHub has a dedicated "Draft" feature
    - [WIP]/[DRAFT] - the changes within the Pull Request are still under active development
    - [DO NOT MERGE] - The Pull Request is not yet ready to be merged, and may need further work.
    - [RFC] - the PR is actively seeking comments and feedback from other developers.
- When the pull request is ready, remove the title prefix using the edit menu.
- Optimally replace with [READY FOR REVIEW]in addition to the above indication.
- The summary should have a meaningful description of what you are trying to accomplish.

#### Pull Request Description
- If you have followed the advice in the commit section, you can usually leave the commit message as the default, which will be the commits from the source branch of the PR.
- Otherwise, please explain in more detail what you are trying to accomplish. If you are working on a draft but want specific feedback, please indicate so in the description.
- If you are working on two projects at once with related changes, please paste a link to that open PR or branch for the reviewer.
- If work is done for a ticket, the GitHub ID should be referenced. THis allows GitHub issues to link together and cross-rference.

#### Start of work:
- A pull request should be created as soon as possible within reason to allow others to understand, review, and collaborate on the change.
    - Pull request should include the Jira ticket numbers (note that multiple tickets can be referenced).
    - The pull request name should summarize the work being done similar to the branch name.
 -  A pull request should be small, concise, easier to review, and understand.
 -  Multiple pull requests can be made from a given branch to meet the above statement.

#### During work:
- The developer will push small commits during the development.
- Commits should be squashed and rebased as necessary for clearer commit history. This should occur while work is progressing and not just once at the end.

#### End of Work:
- Before a pull request is merged, changes should be tested by the developer thorugh both automated and manual exercise of the changed code. 
- Acquiring peer review is the responsibility of the developer and the developer should assign appropriate reviewers for their work. All default reviewers should always be included, do not remove them.
    - Default reviewers should be regularly audited to ensure relevancy to the repository.
    - If you want a specific reviewer, add them to the list instead of replacing the list.
    - If your PR is a draft or wip, it is acceptable to clear the field. In other cases it is not.  
- Further commits, squashes, ad rebases should be coordinated with active revieweers.
- Once the PR is actively being revieewed, do not rewrite history (squash, rebase, etc.) until the PR is complete and approved.
- Ideally, do not push any new commits until the reviewer has marked the PR citing they have finished reviewing.
- Respond to every comment and task made by reviewers.
- Task authors should be responsible for clearing tasks, not PR authors.

#### On pull request merge:
- Once the pull request has been merged, the issue can be assigned to QA testing to verify that the bug has been addressed.
- The developer should assign an appropriate reviewer for their work.
