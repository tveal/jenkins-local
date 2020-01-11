# Repo Orchestrator

- An experiment to manage multiple repos as git-submodules
- run npm commands on submodules concurrently

## Repos

- repo-orchestrator
    - npm-service-a
    - npm-service-b

## Commands

Run these commands from the _repo-orchestrator_ repo

Run a command on the submodule for project a
```
npm run sub:a
```

Run a command on the submodule for project b
```
npm run sub:b
```

Run all sub commands concurrently
```
npm run all
```