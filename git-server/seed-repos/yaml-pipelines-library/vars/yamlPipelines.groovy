import tveal.cicd.jenkins.PipelineGenerator

def call(Script script, Map yaml, List replay=[]) {
    new PipelineGenerator(script, yaml, replay).execute()
}
