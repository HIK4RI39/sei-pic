<template>
    <div id="myTeamSpaceListPage">
        <a-card title="我加入的团队空间">
            <a-table :columns="columns" :data-source="dataList" :loading="loading" :pagination="false">
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'spaceInfo'">
                        <a-space>
                            <a-avatar v-if="record.space?.spaceIcon" :src="record.space.spaceIcon" />
                            <a-avatar v-else shape="square">
                                {{ record.space?.spaceName?.charAt(0) }}
                            </a-avatar>
                            <div style="display: flex; flex-direction: column">
                                <span style="font-weight: bold">{{ record.space?.spaceName }}</span>
                                <a-typography-text type="secondary" size="small">
                                    ID: {{ record.spaceId }}
                                </a-typography-text>
                            </div>
                        </a-space>
                    </template>

                    <template v-else-if="column.dataIndex === 'spaceRole'">
                        <a-tag :color="getRoleTagColor(record.spaceRole)">
                            {{ record.spaceRole === 'admin' ? '管理员' : record.spaceRole === 'editor' ? '编辑者' : '查看者' }}
                        </a-tag>
                    </template>

                    <template v-else-if="column.dataIndex === 'createTime'">
                        {{ dayjs(record.createTime).format('YYYY-MM-DD') }}
                    </template>

                    <template v-else-if="column.key === 'action'">
                        <a-space>
                            <!-- 进入 -->
                            <a-button type="link" @click="router.push(`/space/${record.spaceId}`)">进入</a-button>
                            <!-- 编辑 -->
                            <a-button v-if="record.spaceRole === 'admin'" type="link"
                                @click="openEditModal(record.space)">
                                编辑
                            </a-button>
                            <!-- 退出 -->
                            <a-popconfirm title="确定要退出该团队空间吗？退出后需联系管理员重新邀请。" ok-text="确定" cancel-text="取消"
                                @confirm="handleOutSpace(record.id)">
                                <a-button type="link" danger>退出空间</a-button>
                            </a-popconfirm>
                        </a-space>
                    </template>
                </template>
            </a-table>
        </a-card>

        <!-- 编辑空间 -->
        <a-modal v-model:visible="editModalVisible" title="编辑空间信息" ok-text="确定" cancel-text="取消"
            :confirm-loading="editLoading" @ok="handleEditSubmit">
            <a-form layout="vertical" :model="editFormData">
                <a-form-item label="空间名称" name="spaceName">
                    <a-input v-model:value="editFormData.spaceName" placeholder="请输入新的空间名称" allow-clear />
                </a-form-item>
            </a-form>
        </a-modal>

    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { deleteSpaceUserUsingPost, editSpaceUserUsingPost, listMyTeamSpaceUsingPost } from '@/api/spaceUserController'
import { SPACE_USER_CONFIRM_ENUM } from '@/constants/spaceUser'
import { editSpaceUsingPost } from '@/api/spaceController'
import { reactive } from 'vue'

const router = useRouter()
const loading = ref(false)
const dataList = ref<API.SpaceUserVO[]>([])


// #region 编辑弹窗
// 弹窗状态变量
const editModalVisible = ref(false)
const editLoading = ref(false)

// 编辑表单数据对象
const editFormData = reactive<API.SpaceEditRequest>({
    id: undefined, // 空间 ID
    spaceName: ''   // 空间名称
})

/**
 * 打开编辑弹窗
 * @param spaceVO 空间 VO 数据
 */
const openEditModal = (spaceVO: API.SpaceVO) => {
    if (!spaceVO || !spaceVO.id) {
        message.error('无法获取空间信息')
        return
    }
    // 1. 预填表单数据
    editFormData.id = spaceVO.id
    editFormData.spaceName = spaceVO.spaceName ?? ''
    // 2. 显示弹窗
    editModalVisible.value = true
}

/**
 * 提交编辑修改
 */
const handleEditSubmit = async () => {
    if (!editFormData.id) return
    if (!editFormData.spaceName || editFormData.spaceName.trim() === '') {
        message.warning('空间名称不能为空')
        return
    }

    editLoading.value = true
    try {
        // 假设对应的 API 为 editSpaceUsingPost，需根据实际情况修改
        const res = await editSpaceUsingPost({
            id: editFormData.id,
            spaceName: editFormData.spaceName
        })

        if (res.data.code === 0) {
            message.success('空间名称更新成功')
            editModalVisible.value = false // 关闭弹窗
            fetchData() // 刷新列表，更新表格中展示的名字
        } else {
            message.error('修改失败，' + res.data.message)
        }
    } catch (e) {
        message.error('网络故障')
    } finally {
        editLoading.value = false
    }
}

// #endregion


const columns = [
    {
        title: '空间信息',
        dataIndex: 'spaceInfo',
        key: 'spaceInfo',
    },
    {
        title: '我的角色',
        dataIndex: 'spaceRole',
        key: 'spaceRole',
    },
    {
        title: '加入时间',
        dataIndex: 'createTime',
        key: 'createTime',
    },
    {
        title: '操作',
        key: 'action',
    },
]

/**
 * 加载我加入的空间列表
 */
const fetchData = async () => {
    loading.value = true
    try {
        // 注意：这里必须传一个对象 {} 即使没有过滤条件，防止后端报 Body missing
        const res = await listMyTeamSpaceUsingPost({
            confirmStatus: SPACE_USER_CONFIRM_ENUM.AGREED // 只查询已加入的
        })
        if (res.data.code === 0) {
            dataList.value = res.data.data ?? []
        } else {
            message.error('获取列表失败，' + res.data.message)
        }
    } catch (e) {
        message.error('网络错误，请稍后重试')
    } finally {
        loading.value = false
    }
}

/**
 * 退出空间处理
 * @param id SpaceUser 记录的 ID
 */
const handleOutSpace = async (id: number) => {
    if (!id) return
    const res = await deleteSpaceUserUsingPost({ id })
    if (res.data.code === 0) {
        message.success('已退出空间')
        fetchData() // 刷新列表
    } else {
        message.error('退出失败，' + res.data.message)
    }
}

/**
 * 角色标签颜色转换
 */
const getRoleTagColor = (role: string) => {
    switch (role) {
        case 'admin': return 'green'
        case 'editor': return 'blue'
        default: return 'gray'
    }
}

onMounted(() => {
    fetchData()
})
</script>

<style scoped>
#myTeamSpaceListPage {
    margin-top: 20px;
}
</style>