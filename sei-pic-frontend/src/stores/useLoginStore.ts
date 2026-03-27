import { defineStore } from 'pinia'
import { ref } from 'vue'

const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<any>({
    username: '未登录',
  })

  const fetchLoginUser = async () => {
    // todo 后端接口
    // setTimeout(() => {
    //   loginUser.value = {
    //     id: 1,
    //     userName: 'ceshi',
    //   }
    // }, 3000)
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
