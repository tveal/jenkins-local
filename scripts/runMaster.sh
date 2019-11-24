#!/bin/bash
THIS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JENKINS_MASTER_DIR="$THIS_DIR/../jenkins-master"
JENKINS_HOME_VOLUME="$JENKINS_MASTER_DIR/jenkins_home"

set -e

function main() {
    mkdir -p "$JENKINS_HOME_VOLUME"
    docker run --rm \
        --name jenkins-local-master \
        --network host \
        -v /var/run/docker.sock:/var/run/docker.sock \
        jenkins-local-master:latest
        # -v "$JENKINS_HOME_VOLUME":/var/jenkins_home \
        # -v "$JENKINS_MASTER_DIR/casc_configs":/var/casc_configs \
}

main "$@"