# Repo Orchestrator

- An experiment to manage multiple repos as git-submodules
- run npm commands on submodules concurrently

## Repos

- repo-orchestrator
    - npm-service-a
    - npm-service-b

## Commands

Run these commands from the _repo-orchestrator_ repo

### Run a command on the submodule for project a
```
npm run sub:a
```

### Run a command on the submodule for project b
```
npm run sub:b
```

### Run all sub commands concurrently
```
npm run all
```

### Advanced: run a managed deploy on submodules
```
npm run dp:sub
```
This is `dp:sub` is a POC for deploying groups of repos with deploy-dependencies
- Takes a mapping like:
    ```
    const deployDependencies = {
        'npm-service-a': [],
        'npm-service-b': [],
        'npm-service-c': [
            'npm-service-a',
        ],
        'npm-service-d': [
            'npm-service-a',
            'npm-service-b',
        ],
        'npm-service-e': [
            'npm-service-d',
        ],
        'npm-service-f': [
            'npm-service-c',
        ],
    };
    ```
    and _generates_ deploy groups such as
    ```
    {
        "1": [
            "npm-service-a",
            "npm-service-b"
        ],
        "2": [
            "npm-service-c",
            "npm-service-d"
        ],
        "3": [
            "npm-service-e",
            "npm-service-f"
        ]
    }
    ```
- Takes one deploy group at a time, in order, and runs a sequence of commands
    per repo (repo's in the same group are run concurrently); For example:
    - Group 1:
        1. will run a `build` config for services a and b _concurrently_.
        2. will run a `test` config for services a and b _concurrently_.
    - Group 2: given group 1 succeeded, will repeat process for group 2.
    - Group 3: same as group 2.