#!/bin/bash
THIS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JENKINS_MASTER_DIR="$THIS_DIR/../jenkins-master"
JENKINS_SLAVE_DIR="$THIS_DIR/../jenkins-slave"

set -e

function main() {
    buildMaster
    buildSlave
}

function buildMaster() {
    cd "$JENKINS_MASTER_DIR"
    docker build -t jenkins-local-master .
}

function buildSlave() {
    cd "$JENKINS_SLAVE_DIR"
    docker build -t jenkins-local-slave .
}

main "$@"