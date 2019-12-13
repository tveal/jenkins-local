folder('seed') {
  description('Folder containing all jobs for Jenkins setup')
}
job('seed/jobs') {
  label('docker')
  scm { git { remote { url('git://git-server/jenkins-local-dsl') } } }
  triggers { scm('H/15 * * * *') }
  steps {
    dsl {
      external('src/jobs/**/*.groovy')
    }
  }
}