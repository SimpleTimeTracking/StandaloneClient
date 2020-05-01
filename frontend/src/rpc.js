import { updateActivities } from "./app";

function invoke(arg) {
    window.external.invoke(JSON.stringify(arg));
}

function addActivity(activity) {
    invoke({ cmd: 'executeCommand', activity: activity });
}

function deleteActivity(item) {
    let activity = {};
    activity.activity = item.activity;
    activity.end = item.end ? { 'At': item.end.getTime() / 1000 } : 'Open';
    activity.start = item.start.getTime() / 1000;
    invoke({ cmd: 'deleteActivity', activity: activity });
}

function quitApp() {
    invoke({ cmd: "quit" });
}

function init() {
    if (typeof window.external.invoke === "undefined") {
        console.log("DEMO DATA");
        let demoData = [
            { "start": Date.now() / 1000, "end": "Open", "activity": "ONLY FOR TEST" },
            { "start": Date.now() / 1000 - 86400, "end": "Open", "activity": "ONLY FOR TEST" },
            { "start": 1557945827, "end": { "At": 1557946718 }, "activity": "asdfs" },
            { "start": 1532548282, "end": { "At": 1557945827 }, "activity": "DAF-310332: sdfsdfsdeeeff" },
            { "start": 1517000154, "end": { "At": 1517001931 }, "activity": "aaaa" },
            { "start": 1499195177, "end": { "At": 1508689328 }, "activity": "aaaaa" },
            { "start": 1499195167, "end": { "At": 1499195177 }, "activity": "aaaa" },
            { "start": 1499193850, "end": { "At": 1499195167 }, "activity": "DAF-310332: sdfsdfsdeeeff" },
            { "start": 1499193844, "end": { "At": 1499193850 }, "activity": "DAF-11012: sdfsdfsdff" },
            { "start": 1499193805, "end": { "At": 1499193838 }, "activity": "item 9999 aaa" }
        ];
        for (let i = 1; i < 20000; i++) {
            demoData.push({ "start": 1499193805 - i * 1000000, "end": { "At": 1499193838 - i * 1000000 }, "activity": "item " + i + " aaa" });
        }
        updateActivities(demoData);
    } else {
        invoke({ cmd: "init" });
    }
}

export { addActivity, deleteActivity, quitApp, init };