import { updateActivities } from "./app";
import moment from "moment";

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
        let demoData = [
            { "start": moment().unix(), "end": "Open", "activity": "ONLY FOR TEST" },
            { "start": moment().subtract(1, 'day').unix(), "end": "Open", "activity": "ONLY FOR TEST" },
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
        for (let i = 0; i < 20000; i++) {
            demoData.push({ "start": 1499193805 - i * 1000000, "end": { "At": 1499193838 - i * 1000000 }, "activity": "item " + i + " aaa" });
        }
        updateActivities(demoData);
    } else {
        invoke({ cmd: "init" });
    }
}

export { addActivity, quitApp, init };