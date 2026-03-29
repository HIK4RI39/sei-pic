import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomePage.vue'),
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('@/views/AboutePage.vue'),
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('@/views/user/UserLoginPage.vue'),
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('@/views/user/UserRegisterPage.vue'),
    },
    {
      path: '/admin/user/manage',
      name: '用户管理',
      component: () => import('@/views/user/UserManagePage.vue'),
    },
    // PICTURE
    {
      path: '/picture/add',
      name: '图片上传',
      component: () => import('@/views/picture/AddPicturePage.vue'),
    },
    {
      path: '/picture/:id',
      name: '图片详情',
      component: () => import('@/views/picture/PictureDetailPage.vue'),
      props: true,
    },
    {
      path: '/:pathMatch(.*)*',
      name: '404',
      component: () => import('@/views/404Page.vue'),
    },
  ],
})

export default router
