FROM jenkins/jenkins:lts
USER root
RUN apt-get update && \
    apt-get -y install apt-transport-https \
    ca-certificates \
    curl \
    gnupg2 \
    software-properties-common && \
    curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; echo "$ID")/gpg > /tmp/dkey; apt-key add /tmp/dkey && \
    add-apt-repository \
    "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
    $(lsb_release -cs) \
    stable" && \
    apt-get update && \
    apt-get -y install docker-ce
RUN usermod -a -G docker jenkins
USER jenkins

RUN /usr/local/bin/install-plugins.sh \
    docker-plugin \
    configuration-as-code \
    pipeline-utility-steps \
    matrix-auth \
    cloudbees-folder \
    antisamy-markup-formatter \
    build-timeout \
    credentials-binding \
    timestamper \
    ws-cleanup \
    ant \
    gradle \
    workflow-aggregator \
    github-branch-source \
    pipeline-github-lib \
    pipeline-stage-view \
    git \
    subversion \
    ssh-slaves \
    pam-auth \
    ldap \
    email-ext \
    mailer \
    jdk-tool \
    job-dsl

COPY casc_configs/ /var/casc_configs/
ENV CASC_JENKINS_CONFIG /var/casc_configs/
# https://github.com/jenkinsci/configuration-as-code-plugin/issues/393#issuecomment-408122043
ENV JAVA_OPTS "-Djenkins.install.runSetupWizard=false ${JAVA_OPTS:-}"
