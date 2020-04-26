<template>
  <div class="tabs four" @keydown="checkQuit">
    <input id="tab-1" type="radio" name="tabgroupB" checked />
    <label class="pseudo button toggle" for="tab-1">Activities</label>
    <input id="tab-2" type="radio" name="tabgroupB" />
    <label class="pseudo button toggle" for="tab-2">Daily report</label>
    <input id="tab-3" type="radio" name="tabgroupB" />
    <label class="pseudo button toggle" for="tab-3">Settings</label>
    <input id="tab-4" type="radio" name="tabgroupB" />
    <label class="pseudo button toggle" for="tab-4">Info</label>
    <div class="row">
      <activities ref="command" v-bind:activities="activities"></activities>

      <div></div>

      <div></div>

      <div></div>
    </div>
  </div>
</template>

<script>
import Activities from "./Activities.vue";
import { quitApp } from "./rpc";

export default {
  components: { Activities },
  props: {
    activities: {
      type: Array,
      required: true
    }
  },
  methods: {
    checkQuit: function(evt) {
      if (evt.key === "Escape") {
        quitApp(this.text);
      }
    }
  }
};
</script>

<style>
html,
body {
  height: 100%;
  margin: 0;
  font-size: 1em;
  line-height: normal;
}

@keyframes pulse {
  0% {
    opacity: 0.5;
    transform: scale(0.6);
  }
  50% {
    opacity: 1;
    transform: scale(0.8);
  }
  80% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0.5;
    transform: scale(0.6);
  }
}

.busy-indicator {
  width: 2em;
  height: 2em;
  border-radius: 50%;
  background-color: blue;
  animation: pulse 1s infinite;
  margin: auto;
}
</style>