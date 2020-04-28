import Vue from 'vue';
import picnic from 'picnic';


import App from './App.vue';
import { init } from './rpc';

import { library } from '@fortawesome/fontawesome-svg-core'
import { faStop, faPlay, faTrash, faEdit, faFastForward, faCalendarAlt } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import moment from "moment";

library.add(faStop, faPlay, faTrash, faEdit, faFastForward, faCalendarAlt);
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
    let lastStartDay = null;
    activities.forEach(a => {
        let currentDay = moment.unix(a.start);
        if (lastStartDay == null || !lastStartDay.isSame(currentDay, 'day')) {
            items.push({ 'newday': currentDay });
            lastStartDay = currentDay;
        }
        items.push({
            'activity': {
                start: moment.unix(a.start),
                end: a.end !== 'Open' ? moment.unix(a.end['At']) : null,
                activity: a.activity
            }
        });
    });
    vm.items = items;
}

function commandError(msg) {
    console.log(msg);
}

function commandAccepted() {
    vm.$children[0].$refs.command.clearCommand();
}

export { updateActivities, commandError, commandAccepted };