package tveal.cicd.jenkins

class PipelineGenerator {
    private final Script sc
    private final Map yaml
    private final String cname
    private Map dockerEnv

    PipelineGenerator(Script script, Map yaml) {
        this.sc = script
        this.yaml = yaml
        cname = "build-agent"
    }

    void execute() {
        // https://stackoverflow.com/a/57625568
        try {
            // all normal pipeline action
            setEnvVars()
            sc.stage('Merge Checks') {
                // QA Deploy Safety check
                if (sc.env.CHANGE_TARGET == 'qa' && sc.env.CHANGE_BRANCH != 'master') {
                    sc.error """
                    !!! HALT !!!
                    Pull request to qa detected, but not on master.
                    MUST only pull master into qa.
                    actual source branch: ${sc.env.CHANGE_BRANCH}
                    """
                }
                // Prod Deploy Safety check
                if (sc.env.CHANGE_TARGET == 'prod' && sc.env.CHANGE_BRANCH != 'qa') {
                    sc.error """
                    !!! HALT !!!
                    Pull request to prod detected, but not on qa.
                    MUST only pull qa into prod.
                    actual source branch: ${sc.env.CHANGE_BRANCH}
                    """
                }
                // Robotic commit check
                String botName
                try {
                    botName = yaml.gitBot.name
                } catch(e) {
                    // do nothing. no gitBot so no verification needed
                    botName = ""
                }
                if (botName?.trim() && bashReturn("git log -1 --pretty=format:'%an'") == botName) {
                    // https://stackoverflow.com/a/43889224
                    sc.currentBuild.rawBuild.result = Result.ABORTED
                    throw new hudson.AbortException("Last commit was by gitBot.name: '$botName'; Halting build.")
                }
            }
            sc.stage('Spinup Docker Build Agent') {
                startDockerImage(cname,
                    "${yaml.docker.image}",
                    "${sc.env.WORKSPACE}")
            }
            yaml.pipelines.each { pipeKey, pipeVal ->
                switch(pipeKey) {
                    case 'always':
                        pipeVal.each { stage ->
                            sc.stage(stage.step.name) {
                                stage.step.script.each { cmd ->
                                    runCmdInContainer(cname, "$cmd")
                                }
                            }
                        }
                        break;
                    case 'pullRequests':
                        pipeVal.each { target, stages ->
                            stages.each { stage ->
                                if (sc.env.CHANGE_TARGET == "$target") {
                                    sc.stage(stage.step.name) {
                                        stage.step.script.each { cmd ->
                                            runCmdInContainer(cname, "$cmd")
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case 'branches':
                        pipeVal.each { branch, stages ->
                            stages.each { stage ->
                                if ("${sc.env.BRANCH_NAME}".matches("$branch")) {
                                    sc.stage(stage.step.name) {
                                        stage.step.script.each { cmd ->
                                            runCmdInContainer(cname, "$cmd")
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch(Exception e) {
            throw e
        } finally {
            try {
                removeDockerContainer(cname)
            } catch (Exception e) {
                sc.echo "Running Docker container not found to stop; Something probably failed before starting it."
            }
        }
    }

    //////////////////////////////////////////
    // ENVIRONMENT VARIABLES
    //////////////////////////////////////////
    void setEnvVars() {
        dockerEnv = getDockerEnvMapFromYaml()

        dockerEnv.DEV1_ACCT = '<unique-identifier-for-dev1-account>'
        dockerEnv.DEV2_ACCT = '<unique-identifier-for-dev2-account>'
        dockerEnv.PROD_ACCT = '<unique-identifier-for-prod-account>'

        // https://jenkins.io/doc/pipeline/steps/credentials-binding/
        // sc.withCredentials([sc.string(credentialsId: 'NPM_TOKEN', variable: 'NPM_TOKEN')]) {
        //     dockerEnv.NPM_TOKEN = "${sc.sh(returnStdout: true, script: 'set +x && echo $NPM_TOKEN').trim()}"
        // }

        String actualBranch = sc.env.BRANCH_NAME
        try {
            if (sc.env.CHANGE_BRANCH?.trim()) {
                actualBranch = sc.env.CHANGE_BRANCH
            }
        } catch(e) {
            // do nothing. not a change request
        }
        dockerEnv.ACTUAL_BRANCH = "$actualBranch"

        dockerEnv.each { key, val ->
            // Depending on Jenkins version, requires script security exception
            // sc.env[key] = val
        }
    }

    //////////////////////////////////////////
    // Bash Tools
    //////////////////////////////////////////
    String bashReturn(String cmd) {
        return sc.sh(returnStdout: true, script: "$cmd");
    }

    void bash(String cmd) {
        sc.sh("$cmd");
    }

    //////////////////////////////////////////
    // Docker Functions
    //////////////////////////////////////////
    void startDockerImage(String name, String image, String hostWorkDir) {
        if (image ==~ /^([0-9]+)\.dkr.*/) {
            String ecrAcct = image.split(/\./)[0]
            dockerLogin(ecrAcct)
        }

        String dockerCmd = """set +x
        docker run -d -it ${getDockerAdditionalFlags()} \
            ${getDockerEnvVarsString(dockerEnv)} \
            --network host \
            --name $name \
            -v $hostWorkDir:/home/ubuntu/workspace \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -v /etc/pki/ca-trust/extracted/java/cacerts:/etc/ssl/certs/java/cacerts \
            $image ${getDockerCmd()}"""
        sc.sh "$dockerCmd"
        setDockerGitSshKeyIfNeeded()
        // Since the repo was already cloned by the Jenkins agent via ssh (prereq), turning host-key-check off inside nested docker container
        // is fine... needed for user-prompt issues otherwise
        // https://superuser.com/a/912281
        runCmdInContainerQuiet(name, "set +x && git config --global core.sshCommand \"ssh -o 'StrictHostKeyChecking=no' -i ~/.ssh/id_rsa -F /dev/null\"")
    }

    void runCmdInContainer(String name, String cmd) {
        String sanitizeCmd = cmd.replaceAll("\"", "'")
        sc.sh "docker exec $name /bin/bash -lc \"cd /home/ubuntu/workspace && $sanitizeCmd\""
    }

    void runCmdInContainerQuiet(String name, String cmd) {
        String sanitizeCmd = cmd.replaceAll("\"", "'")
        sc.sh "set +x && docker exec $name /bin/bash -lc \"cd /home/ubuntu/workspace && $sanitizeCmd\""
    }

    void removeDockerContainer(String name) {
        sc.sh "docker rm -f $name"
    }

    String getDockerEnvVarsString(Map env) {
        def array = [];
        env.each { k, v ->
            array << "-e $k='$v'"
        }
        return array.join(" ")
    }

    Map getDockerEnvMapFromYaml() {
        Map dockerEnv = [:]
        try {
            Map projectDockerEnv = yaml.docker.env
            if (projectDockerEnv != null) {
                dockerEnv = projectDockerEnv
            }
        } catch (Exception e) {
            println "INFO: no docker.env set in project pipelines config"
            println e
        }
        return dockerEnv
    }

    String getDockerCmd() {
        String cmd = ""
        try {
            String configCmd = yaml.docker.cmd
            if (configCmd?.trim()) {
                cmd = configCmd
            }
        } catch (e) {
            println "Not using a startup command for docker run"
            println e
        }
        return cmd
    }

    String getDockerAdditionalFlags() {
        String flags = ""
        try {
            String configFlags = yaml.docker.additionalFlags
            if (configFlags?.trim()) {
                flags = configFlags
            }
        } catch (e) {
            println "Not using additional docker flags (user defined)"
            println e
        }
        return flags
    }

    void setDockerGitSshKeyIfNeeded() {
        String defaultMsg = "Not setting SSH key since gitBot.name is not set in project config."
        try {
            if (yaml.gitBot.name?.trim()) {
                // https://stackoverflow.com/a/47627460
                // https://jenkins.io/doc/pipeline/steps/credentials-binding/
                sc.withCredentials([sc.sshUserPrivateKey(credentialsId: 'jenkins-ssh', keyFileVariable: 'keyFile')]) {
                    String key = "${sc.sh(returnStdout: true, script: 'set +x && cat $keyFile')}"
                    // https://stackoverflow.com/a/37779390
                    runCmdInContainerQuiet(cname, "mkdir -p ~/.ssh && echo '$key' > ~/.ssh/id_rsa && chmod 400 ~/.ssh/id_rsa")
                }
            } else {
                sc.echo "$defaultMsg"
            }
        } catch(e) {
            sc.echo "$defaultMsg"
            sc.echo "Reason: $e"
        }
    }

    //////////////////////////////////////////
    // Account Auth
    //////////////////////////////////////////
    void dockerLogin(String ecrAcct) {
        sc.echo "TODO: add your specific impl for private docker login for account $ecrAcct"
    }
}