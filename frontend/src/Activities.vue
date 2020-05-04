<template>
  <div>
    <div>
      <h4>Add or Update Activity</h4>
      <textarea
        id="activity-text"
        ref="activityCommand"
        autofocus="true"
        v-model="text"
        @keydown="checkSubmit"
      ></textarea>
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
          <template v-for="item in matchingItems.slice(0,600)">
            <tr
              class="newday"
              v-show="item.show == version"
              v-if="item.newday"
              :key="'day' + item.newday"
            >
              <td colspan="5">
                <fasi icon="calendar-alt"></fasi>
                {{ as_ll(item.newday)}}
              </td>
            </tr>
            <tr v-show="item.show == version" v-if="item.gap" :key="'gap' + item.gap">
              <td colspan="5">
                <fasi class="error" icon="exclamation-triangle"></fasi>
                <span>Gap between items on same day</span>
              </td>
            </tr>
            <activity-row
              v-show="item.show == version"
              v-if="item.activity"
              v-bind:item="item.activity"
              :key="item.start"
              @selected-activity="setActivity"
              @delete-item="deleteItem"
            ></activity-row>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { addActivity, deleteActivity } from "./rpc";
import ActivityRow from "./ActivityRow";

let tsFormat = new Intl.DateTimeFormat(undefined, {
  weekday: "short",
  year: "numeric",
  month: "short",
  day: "numeric"
});

export default {
  components: { ActivityRow },
  data: function() {
    return {
      text: "",
      version: 0
    };
  },
  computed: {
    matchingItems: function() {
      this.version++;
      let filter = this.text.toLowerCase();

      let j = 0;
      let lastNewDay = null;
      for (let i = 0; i < this.items.length; i++) {
        let a = this.items[i];
        if (a.newday) {
          lastNewDay = a;
        } else if (a.gap) {
          a.show = filter.length < 4 ? this.version : 0;
        } else if (
          filter.length < 4 ||
          a.activity.activity.toLowerCase().indexOf(filter) >= 0
        ) {
          j++;
          if (j >= 600) {
            break;
          }
          a.show = this.version;
          lastNewDay.show = this.version;
        }
      }
      return this.items;
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
      this.$refs.activityCommand.focus();
    },
    deleteItem: function(activity) {
      deleteActivity(activity);
    },
    as_ll: function(dateTime) {
      return tsFormat.format(dateTime);
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

svg.error {
  color: #ff4136;
}
</style>