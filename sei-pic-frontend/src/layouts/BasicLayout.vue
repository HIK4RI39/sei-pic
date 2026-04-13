<template>
    <div id="basicLayout">
        <a-layout style="min-height: 100vh">
            <!-- 导航栏 -->
            <a-layout-header class="header">
                <GlobalHeader />
            </a-layout-header>
            <!-- 侧边栏 -->
            <a-layout>
                <GlobalSider class="sider" />
                <a-layout-content class="content">
                    <router-view />
                </a-layout-content>
            </a-layout>
            <!-- footer -->
            <a-layout-footer class="footer">
                <a href="https://github.com/HIK4RI39/sei-pic" target="_blank">CopyRight @Sei</a>
                <span style="margin: 0 10px; color: #ccc;">|</span>
                <span style="color: rgba(0, 0, 0, 0.45);">
                    违法不良信息举报邮箱：ivlis2@163.com
                </span>
            </a-layout-footer>
        </a-layout>
    </div>

</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from "@/components/GlobalSider.vue";
</script>

<style scoped>
#basicLayout .header {
    padding-inline: 20px;
    background: white;
    color: unset;
    margin-bottom: 1px;
}

#basicLayout .sider {
    background: #fff;
    border-right: 0.5px solid #eee;
    padding-top: 20px;
}

#basicLayout :deep(.ant-menu-root) {
    border-bottom: none !important;
    border-inline-end: none !important;
}

#basicLayout .content {
    padding: 28px;
    background: linear-gradient(to right, #fefefe, #fff);
    margin-bottom: 28px;
}

#basicLayout .footer {
    background: #efefef;
    padding: 16px;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    text-align: center;
}
</style>
