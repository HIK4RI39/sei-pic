<template>
  <div id="app">
    <a-config-provider :locale="zhCN">
      <BasicLayout />
    </a-config-provider>
  </div>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import BasicLayout from '@/layouts/BasicLayout.vue';
import { healthUsingGet } from './api/userController';
dayjs.locale('zh-cn');

// healthUsingGet().then((res) => {
//   console.log(res)
// })


</script>


<style scoped></style>
