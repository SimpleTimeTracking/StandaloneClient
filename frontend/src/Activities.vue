<template>
  <div>
    <div>
      <h4>Add or Update Activity</h4>
      <textarea id="activity-text" autofocus="true" v-model="text" @keydown="checkSubmit"></textarea>
    </div>
    <div>
      <h4>Activities</h4>
      <table id="activity-list">
        <tbody>
          <tr v-for="item in matchingActivities.slice(0, 1000)" :key="item.start">
            <td class="activity">{{item.activity}}</td>
            <td class="time">{{as_lts(item.start)}}</td>
            <td class="symbol">
              <i class="fas fa-fast-forward"></i>
            </td>
            <td class="time">{{as_lts(item.end)}}</td>
            <td class="action"></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { addActivity } from "./rpc";
import * as moment from "moment";
export default {
  data: function() {
    return {
      text: ""
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
    }
  },
  props: ["activities"],
  methods: {
    checkSubmit: function(evt) {
      if (evt.ctrlKey && evt.key === "Enter") {
        addActivity(this.text);
        this.text = "";
      }
    },
    as_lts: function(ts) {
      return ts ? moment.utc(ts).format("LTS") : undefined;
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
  width: 8em;
  text-align: right;
  padding-right: 1em;
}

td.symbol {
  width: 1em;
  padding-right: 1em;
}

td.action {
  width: 1em;
}
</style>