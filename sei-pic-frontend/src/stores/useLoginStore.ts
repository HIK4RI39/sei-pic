import { getLoginUserUsingGet } from '@/api/userController'
import { defineStore } from 'pinia'
import { message } from 'ant-design-vue'
import { ref } from 'vue'

const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<any>({
    username: '未登录',
  })

  const fetchLoginUser = async () => {
    const res = await getLoginUserUsingGet()
    try {
      if (res.data.code === 0 && res.data.data) {
        loginUser.value = res.data.data
      } else {
        message.error('获取登录用户失败' + res.data.message)
      }
    } catch (e: any) {
      message.error('获取登录用户失败' + e.message)
    }
  }

  const setLoginUser = (newLoginUser: any) => {
    loginUser.value = newLoginUser
  }

  return {
    loginUser,
    fetchLoginUser,
    setLoginUser,
  }
})

export { useLoginUserStore }
