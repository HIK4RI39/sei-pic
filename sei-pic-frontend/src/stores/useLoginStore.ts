import { getLoginUserUsingGet } from '@/api/userController'
import { defineStore } from 'pinia'
import { message } from 'ant-design-vue'
import { ref } from 'vue'

const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.UserVO>({
    userName: '未登录',
  })

  const fetchLoginUser = async () => {
    const res = await getLoginUserUsingGet()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
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
