<template>
  <div>
    <div>
      <h4>Add or Update Activity</h4>
      <textarea id="activity-text" autofocus="true" v-model="text" @keydown="checkSubmit"></textarea>
    </div>
    <div>
      <h4>Activities</h4>
      <div v-if="!ready" class="busy-indicator"></div>
      <table v-if="ready" id="activity-list">
        <tbody>
          <tr v-for="item in matchingActivities.slice(0, 1000)" :key="item.start">
            <td class="activity" @click="setActivity(item.activity)">{{item.activity}}</td>
            <td class="time">{{as_lts(item.start)}}</td>
            <td class="symbol">
              <i v-if="item.end !== 'Open'" class="fas fa-fast-forward"></i>
              <div class="running" v-if="item.end === 'Open'"></div>
            </td>
            <td class="time">{{as_lts(item.end)}}</td>
            <td class="action">
              <button class="stop" v-if="item.end">
                <i class="fas fa-stop"></i>
              </button>
              <button class="continue" v-if="!item.end">
                <i class="fas fa-play"></i>
              </button>
              <button class="edit">
                <i class="fas fa-edit"></i>
              </button>
              <button class="error">
                <i class="fas fa-trash"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { addActivity } from "./rpc";
import moment from "moment";
export default {
  data: function() {
    return {
      text: "",
      start_of_day: moment()
        .hour(0)
        .minute(0)
        .second(0)
    };
  },
  computed: {
    matchingActivities: function() {
      let filter = this.text;
      if (filter === "") return this.activities;
      else
        return this.activities.filter(function(a) {
          return a.activity.indexOf(filter) >= 0;
        });
    },
    ready: function() {
      return this.activities;
    }
  },
  props: ["activities"],
  methods: {
    checkSubmit: function(evt) {
      if (evt.ctrlKey && evt.key === "Enter") {
        addActivity(this.text);
      }
    },
    clearCommand: function() {
      this.text = "";
    },
    as_lts: function(ts) {
      if (ts === "Open") return undefined;
      let date = ts["At"] ? moment(ts["At"] * 1000) : moment(ts * 1000);
      let delta = this.start_of_day.diff(date);
      if (delta >= 0 && delta < 86400) {
        return date.format("LTS");
      } else {
        return date.format("lll");
      }
    },
    setActivity: function(activity) {
      this.text = activity;
    }
  }
};
</script>

<style>
#activity-text {
  resize: vertical;
  min-height: 6em;
}
#activity-list {
  width: 100%;
  table-layout: fixed;
}

#activity-list td {
  overflow-wrap: break-word;
}

td.time {
  white-space: nowrap;
  width: 12em;
  text-align: right;
  padding-right: 1em;
}

td.symbol {
  width: 1em;
  padding-right: 1em;
}

td.action {
  width: 6em;
  white-space: nowrap;
}

.action > button {
  margin: 0px;
  padding: 0em 0.3em;
  display: none;
}

tr:hover .action > button {
  display: initial;
}

.running {
  border: 0.3em solid #f3f3f3;
  border-top: 0.3em solid #3498db;
  border-radius: 50%;
  width: 1em;
  height: 1em;
  animation: spin 8s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>