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
            <a-col flex="120px">
                <div class="userLoginStatus">
                    <a-button type="primary" href="/user/login">登录</a-button>
                </div>
            </a-col>
        </a-row>
    </div>
</template>

<script lang="ts" setup>
import { h, ref } from 'vue';
import { HomeOutlined } from '@ant-design/icons-vue';
import { MenuProps } from 'ant-design-vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const current = ref<string[]>(['/']);
const items = ref<MenuProps['items']>([
    {
        key: '/',
        icon: () => h(HomeOutlined),
        label: '主页'
    },
    {
        key: '/about',
        icon: () => h(HomeOutlined),
        label: '关于'
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
</style>