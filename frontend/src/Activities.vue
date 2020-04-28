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
        <colgroup>
          <col />
          <col style="width: 6.8em;" />
          <col style="width: 1em;" />
          <col style="width: 6.8em;" />
          <col style="width: 6em;" />
        </colgroup>
        <tbody>
          <template v-for="item in matchingItems.slice(0, 1000)">
            <tr class="newday" v-if="item.newday" :key="'day' + item.newday">
              <td colspan="5">
                <fasi icon="calendar-alt"></fasi>
                {{ as_ll(item.newday)}}
              </td>
            </tr>
            <activity-row
              v-if="item.activity"
              v-bind:item="item.activity"
              :key="item.start"
              v-on:selected-activity="setActivity"
            ></activity-row>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { addActivity } from "./rpc";
import ActivityRow from "./ActivityRow";
import moment from "moment";

export default {
  components: { ActivityRow },
  data: function() {
    return {
      text: ""
    };
  },
  computed: {
    matchingItems: function() {
      let filter = this.text;
      if (filter.length < 4) return this.items;
      else
        return this.items.filter(function(a) {
          return !a.activity || a.activity.activity.indexOf(filter) >= 0;
        });
    },
    ready: function() {
      return this.items;
    }
  },
  props: ["items"],
  methods: {
    checkSubmit: function(evt) {
      if (evt.ctrlKey && evt.key === "Enter") {
        addActivity(this.text);
      }
    },
    clearCommand: function() {
      this.text = "";
    },
    setActivity: function(activity) {
      this.text = activity;
    },
    as_ll: function(moment) {
      return moment.format("ll");
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

.newday {
  color: cornflowerblue;
}
</style>