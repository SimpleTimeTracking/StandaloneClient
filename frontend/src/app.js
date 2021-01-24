import { html, render } from 'lit-html';
import { repeat } from 'lit-html/directives/repeat.js'
import '@fortawesome/fontawesome-free/css/all.css'
import './w3.css'
import './app.scss'


//import App from './App.vue';
import { init, quitApp, addActivity, continueActivity, deleteActivity, stopActivity } from './rpc';

let now = Math.trunc(Date.now() / 86400000);
let tsFormat = new Intl.DateTimeFormat(undefined, {
    hour: "numeric",
    minute: "numeric",
    second: "numeric"
});

function as_lts(ts) {
    if (!ts) return undefined;
    return tsFormat.format(ts);
};

const vm = {
    activity: ""
};

const commandArea = (activity) => html`
<div>
    <h4>Add or Update Activity</h4>
    <textarea id="activity-text" autofocus="true" @keydown=${checkSubmit} @input=${updateActivity}>${activity}</textarea>
</div>`;

function checkSubmit(evt) {
    if (evt.ctrlKey && evt.key === "Enter") {
        addActivity(vm.activity);
        evt.preventDefault();
        this.value = "";
        updateActivity.call(this);
    }
}

function updateActivity() {
    vm.activity = this.value;
    updateApp();
}

function setActivityText(text) {
    vm.activity = text;
    updateApp();
}

function isOverrun(item) {
    let start = Math.trunc(item.start.getTime() / 86400000);
    return now - start > 0;
}

const activityRow = (item) => html`
<tr>
    <td class="activity" @click=${(_)=> setActivityText(item.activity)}>${item.activity}
    </td>
    <td class="time">${as_lts(item.start)}</td>
    <td class="symbol">
        ${item.end ? 
        html`<i class="fas fa-fast-forward"></i>` : !isOverrun(item) 
        ? html`<div class="running"></div>` : html`<div class="overrun"></div>`}
    </td>
    <td class="time">${as_lts(item.end)}</td>
    <td class="action">
        <div class="w3-bar">
            ${!item.end ? html`<button class="w3-bar-item w3-button w3-padding-small" @click=${() => stopActivity(item)}><i
                    class="fa fa-stop"></i></button>` : ""}
            ${item.end ? html`<button class="w3-bar-item w3-button w3-padding-small" @click=${() => continueActivity(item)}><i
                    class="fa fa-play"></i></button>` : ""}
            <button class="w3-bar-item w3-button w3-padding-small">
                <i class="fa fa-edit"></i>
            </button>
            <button class="w3-bar-item w3-button w3-padding-small" @click=${() => deleteActivity(item)}">
                <i class="fas fa-trash"></i>
            </button>
        </div>
    </td>
</tr>
    `;

const activityList = (items, searchFilter) => {
    searchFilter = searchFilter.length >= 3 ? searchFilter.toLowerCase() : null;
    return html`
    <table class="activity-table">
        <tbody>
            ${repeat(items.filter((it) => it.activity && (!searchFilter || it.activity.activity.toLowerCase().includes(searchFilter))), (item) => item.gap || item.newday || item.start, (item, _) =>
            activityRow(item.activity))}
        </tbody>
    </table>
    `;
}

const activitiesArea = data => html`<div>
    <h4>Activities</h4>
    ${!data.items ? html`<div class="busy-indicator"></div>` : activityList(data.items, data.activity)}
`;


const tabButton = (name, id) => html`<button class="w3-bar-item w3-button" @click=${openingTab(id || name)}> ${name}</button>`;

const app = (data) => html`
    <div>
        <div class="w3-bar w3-black main-menu">
            ${tabButton("Activities")}
            ${tabButton("Daily Report", "DailyReport")}
            ${tabButton("Settings")}
            ${tabButton("Info")}
        </div>
        <div id="Activities" class="tab-content">
            ${commandArea(data.activity)}
            ${activitiesArea(data)}
        </div>
        <div id="DailyReport" class="tab-content" style="display:none">
            <h2>Daily Report</h2>
            <p>Nothing</p>
        </div>
        <div id="Settings" class="tab-content" style="display:none">
            <h2>Settings</h2>
            <p>Nothing</p>
        </div>
        <div id="Info" class="tab-content" style="display:none">
            <h2>Info</h2>
            <p>Nothing</p>
        </div>
    </div>
`;

function appKey(evt) {
    if (evt.key === "Escape") {
        quitApp(this.text);
    }
}

document.addEventListener('keydown', appKey);

function openingTab(tab) {
    return function () {
        for (let element of document.getElementsByClassName("tab-content")) {
            element.style.display = "none";
        }
        document.getElementById(tab).style.display = "block";
    };
}

const appElement = document.getElementById("app");
window.onload = function () {
    init();
    updateApp();
};

function updateApp() {
    render(app(vm), appElement);
}

function updateActivities(activities) {
    let items = [];
    let previousStartDay = null;
    let previousItemOfTodayStart = null;
    activities.forEach(a => {
        let currentDay = new Date(a.start * 1000);
        let currentDayTruncated = Math.trunc(currentDay.getTime() / 86400000);
        if (previousStartDay === null || currentDayTruncated !== previousStartDay) {
            items.push({ 'newday': currentDay });
            previousStartDay = currentDayTruncated;
            previousItemOfTodayStart = null;
        } else {
            if (previousItemOfTodayStart !== null && (a.end !== 'Open' ? a.end['At'] : null) !== previousItemOfTodayStart) {
                items.push({ 'gap': a.start });
            }
        }
        previousItemOfTodayStart = a.start;
        items.push({
            'activity': {
                start: new Date(a.start * 1000),
                end: a.end !== 'Open' ? new Date(a.end['At'] * 1000) : null,
                activity: a.activity
            }
        });
    });
    vm.items = items;
    updateApp();
}

function commandError(msg) {
    console.log(msg);
}

function commandAccepted() {
    //vm.$children[0].$refs.command.clearCommand();
}

export { updateActivities, commandError, commandAccepted, vm };