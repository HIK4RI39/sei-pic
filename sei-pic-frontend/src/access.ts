import { useLoginUserStore } from './stores/useLoginStore'
import router from '@/router'
import { message } from 'ant-design-vue'

let isLoginUserInitialized = false
const whiteList = ['/', '/user/login', '/user/register']

/**
 * 每次切换页面, 都会在进入页面前执行
 */
router.beforeEach(async (to, from, next) => {
  const toPath = to.path
  const isWhiteListed = whiteList.includes(toPath)
  const loginUserStore = useLoginUserStore()

  // 仅当首次调用时, 从后端获取用户信息
  if (!isLoginUserInitialized) {
    try {
      await loginUserStore.fetchLoginUser()
    } catch (e: any) {
      message.error('获取登录态失败,' + e.message)
    }
    // 意为未登录也需要设置, 防止死循环
    isLoginUserInitialized = true
  }

  const loginUser = loginUserStore.loginUser
  const isLogin = !!loginUser

  // 管理员鉴权, 未登录跳转登录重定向toPath, 已登录无权限跳转首页
  if (toPath.startsWith('/admin')) {
    if (loginUser?.userRole !== 'admin') {
      message.error('无权限访问')
      next(isLogin ? '/' : `/user/login?redirect=${to.fullPath}`)
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }

  // 如果不是白名单, 且未登录
  if (!isWhiteListed && !loginUser) {
    message.warning('请先登录')
    next(`/user/login?redirect=${to.fullPath}`)
    return
  }

  // 统一放行
  next()
})
