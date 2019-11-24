# Quickly Spin Up Jenkins Locally

More doc to come... but for now:

**This is NOT in a production-usage state, as security realm is opened up for easier**
**local dev**

This project configures a base Jenkins with:
- Jenkins master node (jenkins/jenkins:lts plus docker and preinstalled plugins)
- Jenkins slave node (jenkinsci/slave:latest plus docker cli)
- Configuration as Code, with Docker Cloud option for spinning up 'docker' labeled
    build agents

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
3. Visit http://localhost:8080/ in a browser

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