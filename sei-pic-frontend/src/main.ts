import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import { createPinia } from 'pinia'
import '@/access'

const app = createApp(App)
const pinia = createPinia()

app.use(router).use(Antd).use(pinia).mount('#app')
