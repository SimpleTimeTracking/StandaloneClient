import Vue from 'vue';
import picnic from 'picnic';


import App from './App.vue';
import { init } from './rpc';

import { library } from '@fortawesome/fontawesome-svg-core'
import { faStop, faPlay, faTrash, faEdit, faFastForward, faCalendarAlt, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

library.add(faStop, faPlay, faTrash, faEdit, faFastForward, faCalendarAlt, faExclamationTriangle);
Vue.component('fasi', FontAwesomeIcon);

let vm = new Vue({
    el: "#app",
    data: function () {
        return {
            items: null,
        }
    },
    render: function (h) {
        return h(App, { attrs: { items: this.items } })
    }
});

window.vm = vm;
window.onload = function () { init(); };

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
    vm.items = Object.freeze(items);
}

function commandError(msg) {
    console.log(msg);
}

function commandAccepted() {
    vm.$children[0].$refs.command.clearCommand();
}

export { updateActivities, commandError, commandAccepted };