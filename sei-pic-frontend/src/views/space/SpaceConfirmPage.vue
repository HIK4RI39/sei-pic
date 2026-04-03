<template>
    <div id="spaceInvitePage">
        <h2>我的空间邀请</h2>
        <div style="margin-bottom: 16px" />

        <a-table :columns="columns" :data-source="teamSpaceList" :loading="loading">
            <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'spaceInfo'">
                    <a-space direction="vertical" size="small">
                        <span style="font-weight: bold">{{ record.space?.spaceName }}</span>
                        <a-tag color="blue">ID: {{ record.spaceId }}</a-tag>
                    </a-space>
                </template>

                <template v-if="column.dataIndex === 'inviteUserInfo'">
                    <a-space v-if="record.user">
                        <a-avatar :src="record.user?.userAvatar" />
                        {{ record.user?.userName }}
                    </a-space>
                    <span v-else>系统邀请</span>
                </template>

                <template v-if="column.dataIndex === 'spaceRole'">
                    <a-tag color="green">{{ record.spaceRole }}</a-tag>
                </template>

                <template v-if="column.dataIndex === 'confirmStatus'">
                    <a-badge v-if="record.confirmStatus === 0" status="processing" text="待处理" />
                    <a-badge v-else-if="record.confirmStatus === 1" status="success" text="已同意" />
                    <a-badge v-else-if="record.confirmStatus === 2" status="error" text="已拒绝" />
                </template>

                <template v-else-if="column.dataIndex === 'createTime'">
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                </template>

                <template v-else-if="column.key === 'action'">
                    <a-space wrap v-if="record.confirmStatus === 0">
                        <a-button type="link" @click="handleInvite(record.id, 1)">同意</a-button>
                        <a-button type="link" danger @click="handleInvite(record.id, 2)">拒绝</a-button>
                    </a-space>
                    <span v-else>--</span>
                </template>
            </template>
        </a-table>
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { confirmSpaceUserUsingPost, listMyTeamSpaceUsingPost } from '@/api/spaceUserController';
import { SPACE_USER_CONFIRM_ENUM } from '@/constants/spaceUser';

// 表格列定义
const columns = [
    {
        title: '邀请空间',
        dataIndex: 'spaceInfo',
    },
    {
        title: '邀请人',
        dataIndex: 'inviteUserInfo',
    },
    {
        title: '拟分配角色',
        dataIndex: 'spaceRole',
    },
    {
        title: '状态',
        dataIndex: 'confirmStatus',
    },
    {
        title: '邀请时间',
        dataIndex: 'createTime',
    },
    {
        title: '操作',
        key: 'action',
    },
]

const teamSpaceList = ref<API.SpaceUserVO[]>([])
const loading = ref(false)

/**
 * 获取我的邀请列表
 */

const fetchTeamSpaceList = async () => {
    loading.value = true

    const res = await listMyTeamSpaceUsingPost({
        confirmStatus: SPACE_USER_CONFIRM_ENUM.CONFIRMING
    })
    if (res.data.code === 0 && res.data.data) {
        teamSpaceList.value = res.data.data
    } else {
        message.error('加载我的团队空间失败，' + res.data.message)
    }

    loading.value = false

}



/**
 * 处理邀请（同意/拒绝）
 * @param id 记录ID
 * @param status 1-同意, 2-拒绝
 */
const handleInvite = async (id: number, status: number) => {
    if (!id) return

    const res = await confirmSpaceUserUsingPost({
        id,
        confirmStatus: status,
    })

    if (res.data.code === 0) {
        message.success(status === 1 ? '已加入空间' : '已拒绝邀请')
        fetchTeamSpaceList() // 刷新列表
    } else {
        message.error('处理失败，' + res.data.message)
    }
}

onMounted(() => {
    fetchTeamSpaceList()
})
</script>

<style scoped>
#spaceInvitePage {
    background: #fff;
    padding: 24px;
    border-radius: 8px;
}
</style>