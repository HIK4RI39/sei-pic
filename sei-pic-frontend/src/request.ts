import { message } from 'ant-design-vue'
import axios from 'axios'

// 由vite配置转发/api到后端
const DEV_BASE_URL = 'http://localhost:8123'
const PROD_BASE_URL = 'http://42.193.172.173'
const myAxios = axios.create({
  baseURL: DEV_BASE_URL,
  // baseURL: PROD_BASE_URL,
  timeout: 5000,
  withCredentials: true,
})

// 添加请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // 在发送请求之前做些什么
    return config
  },
  function (error) {
    // 对请求错误做些什么
    return Promise.reject(error)
  },
)

// 添加响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    // 从response从取出data
    const { data } = response
    // 未登录, 跳转登录页 (白名单登录注册)
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login/?redirect=${window.location.href}}`
      }
    }
    return response
  },
  function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    message.error(error.message)
    return Promise.reject(error)
  },
)

export default myAxios
