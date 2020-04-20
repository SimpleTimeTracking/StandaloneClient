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
      <activities v-bind:activities="activities"></activities>

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
}
</style>