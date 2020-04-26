import Vue from 'vue';
import picnic from 'picnic';
import fontawesome from '@fortawesome/fontawesome-free/css/all.css';

import App from './App.vue';
import { init } from './rpc';

let vm = new Vue({
    el: "#app",
    data: function () {
        return {
            activities: null
        }
    },
    render: function (h) {
        return h(App, { attrs: { activities: this.activities } })
    }
});

window.vm = vm;
window.onload = function () { init(); };

function updateActivities(activities) {
    vm.activities = activities;
}

function commandError(msg) {
    console.log(msg);
}

function commandAccepted() {
    vm.$children[0].$refs.command.clearCommand();
}

export { updateActivities, commandError, commandAccepted };