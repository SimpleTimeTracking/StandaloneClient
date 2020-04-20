import Vue from 'vue';
import picnic from 'picnic';
import fontawesome from '@fortawesome/fontawesome-free/css/all.css';

import App from './App.vue';
import { init } from './rpc';

let vm = new Vue({
    el: "#app",
    data: function () {
        return {
            activities: [{
                start: 2, end: 3, activity: "ttt"
            }, {
                start: 1, activity: "ttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt"
            }, { "start": 1399089839, "end": 1399093439, "activity": "item 0" }]
        }
    },
    render: function (h) {
        return h(App, { attrs: { activities: this.activities } })
    }
});

window.onload = function () { init(); };

function updateActivities(activities) {
    vm.activities = activities;
}

export { updateActivities };