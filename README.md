# Quickly Spin Up Jenkins Locally

**This is NOT in a production-usage state, as security realm is opened up for easier**
**local dev**

This project configures a base Jenkins with:
- Jenkins master node (jenkins/jenkins:lts plus docker and preinstalled plugins)
- Jenkins slave node (jenkinsci/slave:latest plus docker cli)
- Configuration as Code
    - Docker Cloud option for spinning up 'docker' labeled build agents
    - Seed Job: Using Job-DSL, creates a "seed/jobs" job that points to the
        jenkins-local-dsl repo to generate more jobs; This way you can control
        project jobs from the dsl repo
- Local git server for testing with code repositories

Prerequisites
- Docker

## Build and Run

1. Build the needful (first time can take a WHILE - builds 2 images)
    ```bash
    docker-compose build
    ```
2. Run the needful
    ```bash
    docker-compose up
    ```
3. Visit http://localhost:8080/ in a browser; To trigger builds/manage Jenkins,
    user/pass: admin

## Remove Persistent Volume

The docker-compose sets up a jenkins_data volume for persistent Jenkins config.
To remove this volume and start over from a clean slate:

1. List the volumes matching the filter
    ```bash
    docker volume ls | grep jenkins_data
    ```
2. Remove the volume by name (change accordingly to info from the last command)
    ```bash
    docker volume rm jenkins-local_jenkins_data
    ```

## Local Git Server

With the docker-compose stack running, you can clone the repo(s) that are in the
local git server

```bash
cd /tmp && git clone git://localhost/project-a
```

On startup (aka, during `docker-compose up`), the git-server initiates git repos
from the folders in the [git-server/seed-repos](git-server/seed-repos/). You can
then clone the repo(s), change, commit, and push them as needed while the server
is running. Restart the server and start over from the seed-state.

## Stop/Start

Currently, the git-server cannot handle start/stop of the container, so you have
to make sure to remove the container before starting again; simply run:

```bash
docker-compose down
```

Then you can run the needful again:
```bash
docker-compose up
```
