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
    if (typeof window.external.invoke === "undefined") {
        console.log("DEMO DATA");
        window.vm.activities = [
            { "start": 1557946718, "end": "Open", "activity": "euaoeu" },
            { "start": 1557945827, "end": { "At": 1557946718 }, "activity": "asdfs" },
            { "start": 1532548282, "end": { "At": 1557945827 }, "activity": "DAF-310332: sdfsdfsdeeeff" },
            { "start": 1517000154, "end": { "At": 1517001931 }, "activity": "aaaa" },
            { "start": 1499195177, "end": { "At": 1508689328 }, "activity": "aaaaa" },
            { "start": 1499195167, "end": { "At": 1499195177 }, "activity": "aaaa" },
            { "start": 1499193850, "end": { "At": 1499195167 }, "activity": "DAF-310332: sdfsdfsdeeeff" },
            { "start": 1499193844, "end": { "At": 1499193850 }, "activity": "DAF-11012: sdfsdfsdff" },
            { "start": 1499193838, "end": { "At": 1499193844 }, "activity": "DAF-10012: sdfsdf" },
            { "start": 1499193805, "end": { "At": 1499193838 }, "activity": "item 9999 aaa" }
        ];
    } else {
        invoke({ cmd: "init" });
    }
}

export { addActivity, quitApp, init };