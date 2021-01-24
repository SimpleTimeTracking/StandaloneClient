<template>
</template>

<script>
import Activities from "./Activities";
import { quitApp } from "./rpc";

export default {
  components: { Activities },
  props: {
    items: {
      type: Array
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
  font-size: 0.95em;
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