# YAML Pipelines Jenkins Library

Goal: Use a `jenkins-pipelines.yml` file per project to configure CI/CD.

Key Factors:
- Run CI/CD commands inside a specified docker image
- Simple yaml config to define pipeline stages

Prerequisites
- Your Jenkins instance must have the
    [pipeline-utility-steps](https://plugins.jenkins.io/pipeline-utility-steps) plugin.
    Also see [this](https://jenkins.io/doc/pipeline/steps/pipeline-utility-steps/#readyaml-read-yaml-from-files-in-the-workspace-or-text).
- If you need to push changes back to git repo, you must have the Jenkins job setup
    to checkout over SSH. Details on this further down in this doc.
- Your Jenkins instance must have a build agent with docker installed.

## Implement in a Project

You need to add 2 files to your project:
- **Jenkinsfile** (minimal config to pull this library, checkout the code and invoke
    the library)
- **jenkins-pipelines.yml** Configure the docker build agent and pipelines

1. Add a Jenkinsfile to your project, with the following config
    (change the repo url/name as needed):
    ```groovy
    library(
        identifier: 'yaml-pipelines-library@master',
        retriever: modernSCM(
            [
                $class: 'GitSCMSource',
                remote: 'https://git-server/yaml-pipelines-library',
                // credentialsId: 'jenkins-creds-id-for-git-repo-auth'
            ]
        )
    )

    node('docker') { // label of your build-agent that has docker
        stage('Checkout') {
            checkout scm
        }
        yamlPipelines(this, readYaml(file: 'jenkins-pipelines.yml'))
    }
    ```
2. Add a jenkins-pipelines.yml file with config like the following:
    ```yaml
    docker:
      image: node:8
      additionalFlags: --user 1000:1000

    pipelines:
      always:
        - step:
            name: Build
            script:
              - npm ci
              - npm test
      pullRequests:
        master:
          - step:
              name: Deploy to DEV
              script:
                - DEPLOY_ENV=$DEV1_ACCT npm run dp:dev1
      branches:
        master:
          - step:
              name: Deploy to PROD
              script:
                - DEPLOY_ENV=$PROD_ACCT npm run dp:prod
    ```

## Variations with Docker Config

**All the options**
```yaml
docker:
  image: node:8
  cmd: bash
  env:
    VAR1: variable 1
    VAR2: variable 2
  additionalFlags: --user 1000:1000
```
- **docker.image** (_required_, String) - can be publicly available docker-hub image
    or a privately hosted image. If it's a private registry, the library will attempt to
    docker-login.
- **docker.cmd** (_optional_, String) - the command to run in
    `docker run -d -it $image $cmd`; This depends on the image you specify
- **docker.env** (_optional_, Map) - Static environment variables to be passed into the
    docker container
- **docker.additionalFlags** (_optional_, String) - additional flags to add to the
    `docker run -d -it $additionalFlags $image` command. NOTE: env variables, network,
    workspace, and docker-sock config are already set.

## Enable Pushing Changes Back to Git Repo

If you have a library project, such as a npm library or some other project that
needs auto-version bumping, you need:
1. Make sure your Jenkins job checks out the repo with SSH.
2. Set the `gitBot.name` in the jenkins-pipelines.yml
    ```yaml
    gitBot:
      name: jenkins-robotic-user
    ```
    Make sure in your git-push stage that you set the
    **git user to this same value**. The library will abort builds where the
    last commit was made with this author - this way, there will be no infinite
    loop when Jenkins auto-bumps a repo version

## Pipelines Config

There are 3 types of pipelines:
1. **pipelines.always** - Stages that run without a conditional
2. **pipelines.pullRequests** - Stages that run on a pull-request against a specified
    target branch
3. **pipelines.branches** - Stages that run when on a specified branch

Structure: pipelines.always
```yaml
pipelines:
  always:
    - step: # aka Jenkins stage
        name: Build # name of the stage
        script:
          - echo "this is a command to run in the given stage"
          - echo "add as many commands as you need per stage"
```

Structure: pipelines.pullRequests
```yaml
pipelines:
  pullRequests:
    master: # target branch, PR's against master
      - step: # aka Jenkins stage
          name: Code Quality Scans # name of the stage
          script:
            - echo "this is a command to run in the given stage"
            - echo "add as many commands as you need per stage"
      - step: # another stage for PR's against master
          name: Deploy to DEV1
          script:
            - DEPLOY_ENV=$DEV1_ACCT npm run dp:dev1
```

Structure: pipelines.branches
```yaml
pipelines:
  branches:
    master: # current branch; only do following stage(s) on master branch
      - step: # aka Jenkins stage
          name: Deploy to PROD # name of the stage
          script:
            - DEPLOY_ENV=$PROD_ACCT npm run dp:prod
```