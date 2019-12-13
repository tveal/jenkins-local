import tveal.cicd.jenkins.PipelineGenerator

def call(Script script, Map yaml) {
    new PipelineGenerator(script, yaml).execute()
}
