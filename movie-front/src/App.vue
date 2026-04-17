<template>
  <router-view></router-view>
</template>

<script setup>
// 这里不需要写什么逻辑
</script>

<style>
/* 去除浏览器默认的边距 */
body, html {
  margin: 0;
  padding: 0;
  height: 100%;
}
#app {
  height: 100%;
}
</style>