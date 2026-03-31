import { createApp, defineComponent } from 'vue'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import { createPinia } from 'pinia'
import '@/access'
import VueCropper from 'vue-cropper'
import 'vue-cropper/dist/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(router).use(Antd).use(pinia).use(VueCropper)
app.mount('#app')
