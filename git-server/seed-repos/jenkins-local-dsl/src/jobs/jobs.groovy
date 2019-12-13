def repos = [
    "project-a"
]

def newRepos = []
repos.each { repoName ->
    if (jenkins.model.Jenkins.instance.getItemByFullName("$repoName") == null){
        newRepos.add(repoName)
    }

    multibranchPipelineJob(repoName) {
        branchSources {
            git {
                id(repoName) // IMPORTANT: use a constant and unique identifier
                remote("git://git-server/$repoName")
                // credentialsId('github-ci')
                // includes('JENKINS-*')
            }
        }
        orphanedItemStrategy {
            discardOldItems {
                numToKeep(20)
            }
        }
    }
}

newRepos.each { newRepo ->
    println "Adding new job to the queue: $newRepo"
    queue(newRepo)
}
