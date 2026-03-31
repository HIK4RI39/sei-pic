<template>
    <div id="mySpace">
        <p>正在跳转，请稍候...</p>
    </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSpaceVoUsingPost } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 检查用户是否有个人空间
const checkUserSpace = async () => {
    const loginUser = loginUserStore.loginUser
    if (!loginUser?.id) {
        router.replace('/user/login')
        return
    }
    // 获取用户空间信息
    const res = await getSpaceVoUsingPost()
    if (res.data.code === 0) {
        if (res.data.data) {
            // 跳转个人空间
            router.replace(`/space/${res.data.data.id}`)
        } else {
            // 跳转创建空间
            router.replace('/space/create')
            message.warn('请先创建空间')
        }
    } else {
        message.error('加载我的空间失败，' + res.data.message)
    }
}

// 在页面加载时检查用户空间
onMounted(() => {
    checkUserSpace()
})
</script>
