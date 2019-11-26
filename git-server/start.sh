#!/usr/local/bin/bash

set -e

function main() {
    startGitServer
    seedRepos

    # purely to keep the shell alive
    touch ~/git.log
    tail -f ~/git.log
}

function startGitServer() {
    # https://www.maketecheasier.com/run-bash-commands-background-linux/
    # nohup git quickserve &>~/git.log &

    # continues to stream output to active shell
    git quickserve &
}

function seedRepos() {
    for repo in $(find /git/seed-repos -type d -maxdepth 1 -mindepth 1); do
        repoName="$(basename $repo)"
        remoteDir="/git/remote/$repoName"
        
        # Create Remote
        mkdir "$remoteDir"
        cd "$remoteDir"
        git init --bare
        
        # Clone bare repo, seed, and push
        cd /git/repos
        git clone "git://git-server/$repoName"
        cp -R "$repo/." "$repoName/"
        cd "$repoName"
        git add .
        git commit -m 'Initial Commit. Add the seed data.'
        git push origin master
    done
}

main "$@"