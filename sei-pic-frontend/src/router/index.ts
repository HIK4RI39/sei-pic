import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomePage.vue'),
    },
    // 用户
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
      component: () => import('@/views/admin/UserManagePage.vue'),
    },
    // PICTURE
    {
      path: '/picture/add',
      name: '图片上传',
      component: () => import('@/views/picture/AddPicturePage.vue'),
    },

    {
      path: '/admin/pic_add/batch',
      name: '批量创建图片',
      component: () => import('@/views/picture/BatchAddPicturePage.vue'),
    },

    {
      path: '/picture/:id',
      name: '图片详情',
      component: () => import('@/views/picture/PictureDetailPage.vue'),
      props: true,
    },
    {
      path: '/admin/pictureManage',
      name: '图片管理',
      component: () => import('@/views/admin/PictureManagePage.vue'),
    },
    // 空间
    {
      path: '/admin/spaceManage',
      name: '空间管理',
      component: () => import('@/views/admin/SpaceManagePage.vue'),
    },
    {
      path: '/space/create',
      name: '创建空间',
      component: () => import('@/views/space/SpaceCreatePage.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: '404',
      component: () => import('@/views/404Page.vue'),
    },
  ],
})

export default router
