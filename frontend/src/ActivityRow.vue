<template>
  <tr>
    <td class="activity" @click="$emit('selected-activity', item.activity)">{{item.activity}}</td>
    <td class="time">{{as_lts(item.start)}}</td>
    <td class="symbol">
      <fasi v-if="item.end" icon="fast-forward"></fasi>
      <div class="running" v-if="!item.end && !isOverrun(item)"></div>
      <div class="overrun" v-if="!item.end && isOverrun(item)"></div>
    </td>
    <td class="time">{{as_lts(item.end)}}</td>
    <td class="action">
      <button class="stop" v-if="item.end">
        <fasi icon="stop"></fasi>
      </button>
      <button class="continue" v-if="!item.end">
        <fasi icon="play"></fasi>
      </button>
      <button class="edit">
        <fasi icon="edit"></fasi>
      </button>
      <button class="error" @click="$emit('delete-item', item)">
        <fasi icon="trash"></fasi>
      </button>
    </td>
  </tr>
</template>

<script>
let now = Math.trunc(Date.now() / 86400000);
let tsFormat = new Intl.DateTimeFormat(undefined, {
  hour: "numeric",
  minute: "numeric",
  second: "numeric"
});

export default {
  props: ["item"],
  methods: {
    as_lts: function(ts) {
      if (!ts) return undefined;
      return tsFormat.format(ts);
    },
    isOverrun: function(item) {
      let start = Math.trunc(item.start.getTime() / 86400000);
      return now - start > 0;
    }
  }
};
</script>

<style>
#activity-list td {
  overflow-wrap: break-word;
}

td.time {
  white-space: nowrap;
  text-align: right;
  padding-right: 0.2em;
  padding-left: 0.2em;
}

td.symbol {
  padding-right: 1em;
}

td.action {
  white-space: nowrap;
  padding-left: 0.2em;
  padding-right: 0.2em;
}

.action > button {
  margin: 0px;
  padding: 0em 0.2em;
  display: none;
}

tr:hover .action > button {
  display: initial;
}

.running {
  border: 0.3em solid #00000000;
  border-top: 0.25em solid cornflowerblue;
  border-radius: 50%;
  width: 1em;
  height: 1em;
  animation: spin 8s linear infinite;
}

.overrun {
  border: 0.3em solid #00000000;
  border-top: 0.25em solid red;
  border-left: 0.25em solid red;
  border-radius: 50%;
  width: 1em;
  height: 1em;
  animation: spin 2s ease infinite;
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