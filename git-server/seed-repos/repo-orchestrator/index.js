const concurrently = require('concurrently');

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

const getDeps = (deployDeps, repoDeps) => {
    let weight = 0;
    if (repoDeps.length > 0) {
        weight++;
        repoDeps.forEach((repo) => {
            weight += getDeps(deployDeps, deployDeps[repo]);
        });
    }
    return weight;
};

const createDeployGroups = () => {
    let deployGroups = {};
    Object.keys(deployDependencies).forEach((repo) => {
        let weight = 1 + getDeps(deployDependencies, deployDependencies[repo]);
        if (!deployGroups[weight]) {
            deployGroups[weight] = [];
        }
        deployGroups[weight].push(repo)
    });
    return deployGroups;
};

const runCmdConcurrentlyOnRepos = (cmd, uow) => {
    const { repos, group } = uow;
    console.log('\n########################');
    console.log(`GROUP ${group} >>---> ${cmd}`);
    console.log(JSON.stringify(repos, null, 2));
    console.log('########################\n');
    let commands = [];
    repos.forEach((repo) => {
        commands.push({
            command: `npm --prefix ${repo} run ${cmd}`,
            name: repo
        });
    });
    return new Promise((resolve, reject) => {
        concurrently(commands, {
            // prefix: cmd,
            killOthers: ['failure'],
        })
        .then(() => resolve({
            ...uow,
            status: `Completed ${cmd} on repos in Group ${group}`,
        }))
        .catch(() => reject(`FAILED to run ${cmd} on repo group ${group}: ${JSON.stringify(repos)}`));
    });
};

const build = (uow) => {
    return runCmdConcurrentlyOnRepos('build', uow);
};

const test = (uow) => {
    return runCmdConcurrentlyOnRepos('test', uow);
};

const main = async () => {
    const deployGroups = createDeployGroups();
    console.log(JSON.stringify(deployGroups, null, 2));
    
    try {
        for (const group of Object.keys(deployGroups)) {
            await build({
                group,
                repos: deployGroups[group],
            })
            .then(test)
            .then((uow) => console.log(uow.status));
            // don't catch error here so error will short-circuit the loop
        };
    } catch (err) {
        console.log(err);
    }
};

main();
