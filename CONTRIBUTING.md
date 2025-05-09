## Rules for Contributing

_Content to be added_

## Developer Workflow Overview

- Assign an issue to a resource
- Create a new branch to address the assigned issue
- Address the issue
- Commit changes
- Open a pull request
- Have a peer review the pull request
    - A pull request may require revisions from the reviewer before being approved 
- Merge approved pull request into the main branch
- Delete the working branch if no longer needed

## Coding Standards and Style Guides

_Content to be added_

## Branching Strategies

_Content to be added_

## Git Operations and Pull Requests Guidelines

### Branching

- All branches should have a known goal and lifecycle.
- The branch name should summarize its goal and be appended by:
    - devops/ - branches with changes that do not actually affect source code
    - feature/ - branches with new features
    - bugfix/ - branches with bug fixes
    - docs/ - for branches only containing documentation addition or edits
- Branches should be lightweight according to the goal and lifecycle.
- **Main branch must remain deployable at all times.**
    - No pull request should be maid to main with partially completed work (unless coordinated beforehand).
    - Deployable means:
        - Developer tested
        - Automated tests are passing
        - Feature complete
        - Peer-reviewed and accepted
        - Static analysis completed
        - Test coverage metrics goal met
- **Other people's branches:**
  1. Do not push commits to someone else's feature branch; at least not without coordinating with them first.
     1. It is somewhat rude.
     2. There could be local changes that the originator has not pushed up yet that you are missing.
  2. Instead submit a PR to that branch if you feel you have a constructive change for it.
- **Branches in general**
    - Do not leave feature branches laying around.
- **Avoid Git Force Pushes**
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
- Once the pull request has been merged, the issue can be assigned to QA testing to verify that the bug has been addressed. Status should be 
