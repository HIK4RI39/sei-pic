<template>
    <div id="globalHeader">
        <a-row :wrap="false">
            <a-col flex="200px">
                <router-link to="/">
                    <div class="title-bar">
                        <img class="logo" src="../../public/favicon.png" alt="logo">
                        <div class="title">猫图库</div>
                    </div>
                </router-link>
            </a-col>
            <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick" />
            <a-col flex="auto">
            </a-col>
            <!-- 用户信息 -->
            <a-col flex="150px">
                <div class="userLoginStatus">
                    <!-- dropdown -->
                    <a-dropdown>
                        <a-space>
                            <a-avatar :src="loginUserStore?.loginUser?.userAvatar" />
                            {{ userInfo.userName ?? '无名' }}
                        </a-space>
                        <template #overlay>
                            <a-menu>
                                <a-menu-item @click="doLogOut" style="color: red;">
                                    <logout-outlined />
                                    <a href="javascript:" style="margin-left: 5px;">退出登录</a>
                                </a-menu-item>
                            </a-menu>
                        </template>
                    </a-dropdown>
                    <a-button v-if="!loginUserStore?.loginUser" type="primary" ghost href="/user/login"
                        style="margin-left: 5px;">登录</a-button>
                </div>
            </a-col>
        </a-row>
    </div>
</template>

<script lang="ts" setup>
import { h, ref } from 'vue';
import { InsertRowAboveOutlined, HomeOutlined, LogoutOutlined } from '@ant-design/icons-vue';
import { MenuProps, message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { useLoginUserStore } from '@/stores/useLoginStore';
import { logoutUsingGet } from '@/api/userController';

const router = useRouter();
const current = ref<string[]>(['/']);
const items = ref<MenuProps['items']>([
    {
        key: '/',
        icon: () => h(HomeOutlined),
        label: '主页'
    },
    {
        key: '/user/manage',
        icon: () => h(InsertRowAboveOutlined),
        label: '用户管理'
    }
]);

/**
 * 跳转菜单页
 * @param key 菜单项
 */
const doMenuClick = ({ key }) => {
    router.push({
        path: key
    })
}

router.afterEach((to, from, next) => {
    current.value = [to.path]
})

const loginUserStore: any = useLoginUserStore()
const userInfo = loginUserStore.loginUser

const doLogOut = async () => {
    const res = await logoutUsingGet()
    try {
        if (res.data.code === 0 && res.data.data) {
            message.success("登出成功,")
            await router.push('/user/login')
        } else {
            message.error("登出失败," + res.data.message)
        }
    } catch (e: any) {
        message.error("登出失败," + e.message);
    }
}


</script>
<style scoped>
#globalHeader .title-bar {
    display: flex;
    align-items: center;
}

.title {
    color: black;
    font-size: 18px;
    margin-left: 16px;

}

.logo {
    height: 48px;
}

.userLoginStatus {
    display: flex;
    align-items: right;
}
</style>