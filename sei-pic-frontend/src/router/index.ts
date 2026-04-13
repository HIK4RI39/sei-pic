import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // {
    //   path: '/resume',
    //   name: 'resume',
    //   component: () => import('@/resume.vue'),
    // },
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
    // 个人资料
    {
      path: '/user/profile',
      name: '个人资料',
      component: () => import('@/views/user/UserEditPage.vue'),
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
    {
      path: '/search_picture',
      name: '图片搜索',
      component: () => import('@/views/picture/SearchPicturePage.vue'),
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
      props: true,
    },
    {
      path: '/my_space',
      name: '我的空间',
      component: () => import('@/views/space/MySpacePage.vue'),
    },
    {
      path: '/space/:id',
      name: '空间详情',
      component: () => import('@/views/space/SpaceDetailPage.vue'),
      props: true,
    },
    {
      path: '/space_analyze',
      name: '空间分析',
      component: () => import('@/views/space/SpaceAnalyzePage.vue'),
      props: true,
    },
    {
      path: '/spaceUserManage/:id',
      name: '团队空间成员管理',
      component: () => import('@/views/space/SpaceUserManagePage.vue'),
      props: true,
    },

    // my
    {
      path: '/space/confirm',
      name: '团队邀请确认',
      component: () => import('@/views/my/SpaceConfirmPage.vue'),
    },
    {
      path: '/space/team/list',
      name: '管理已加入的团队空间列表',
      component: () => import('@/views/my/MyTeamSpaceListPage.vue'),
    },
    {
      path: '/my/pictures',
      name: '用户上传到公共图库的照片',
      component: () => import('@/views/my/MyPicturesPage.vue'),
    },
    {
      path: '/space/team/list',
      name: '管理已加入的团队空间列表',
      component: () => import('@/views/my/MyTeamSpaceListPage.vue'),
    },
    {
      path: '/user/exchangeVip',
      name: '兑换VIP',
      component: () => import('@/views/my/ExchangeVipPage.vue'),
    },

    {
      path: '/:pathMatch(.*)*',
      name: '404',
      component: () => import('@/views/404Page.vue'),
    },
  ],
})

export default router
