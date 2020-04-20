function invoke(arg) {
    window.external.invoke(JSON.stringify(arg));
}

function addActivity(activity) {
    invoke({ cmd: 'addActivity', activity: activity });
}

function quitApp() {
    invoke({ cmd: "quit" });
}

function init() {
    invoke({ cmd: "init" });
}

export { addActivity, quitApp, init };