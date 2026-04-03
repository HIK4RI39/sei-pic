<template>
    <div id="spaceUserManagePage">
        <h2>空间成员管理</h2>
        <a-form layout="inline" :model="formData" @finish="handleSubmit">
            <a-form-item label="用户 id" name="userId">
                <a-input v-model:value="formData.userId" placeholder="请输入用户 id" allow-clear />
            </a-form-item>
            <a-form-item>
                <a-button type="primary" html-type="submit">添加用户</a-button>
            </a-form-item>
        </a-form>


        <div style="margin-bottom: 16px" />
        <!-- 空间信息 -->
        <a-table :columns="columns" :data-source="dataList">
            <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'userInfo'">
                    <a-space>
                        <a-avatar :src="record.user?.userAvatar" />
                        {{ record.user?.userName }}
                    </a-space>
                </template>
                <!-- 修改权限 -->
                <template v-if="column.dataIndex === 'spaceRole'">
                    <a-select v-model:value="record.spaceRole" :options="SPACE_ROLE_OPTIONS"
                        :disabled="record.space.userId == record.userId"
                        @change="(value) => editSpaceRole(value, record)" />
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                </template>
                <!-- 用户id -->
                <template v-if="column.dataIndex === 'userId'">
                    <a-space>
                        {{ record.userId }}
                    </a-space>
                </template>
                <!-- 邀请人 -->
                <template v-if="column.dataIndex === 'createUserId'">
                    <a-space>
                        {{ record.createUserId }}
                    </a-space>
                </template>
                <!-- 删除 -->
                <template v-else-if="column.key === 'action'">
                    <a-space wrap>
                        <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
                    </a-space>
                </template>
            </template>
        </a-table>
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { addSpaceUserUsingPost, deleteSpaceUserUsingPost, editSpaceUserUsingPost, listSpaceUserUsingPost } from '@/api/spaceUserController';
import { reactive } from 'vue';
import { SPACE_ROLE_OPTIONS } from '@/constants/space';

// 表格列
const columns = [
    {
        title: '用户',
        dataIndex: 'userInfo',
    },
    {
        title: '角色',
        dataIndex: 'spaceRole',
    },

    {
        title: 'id',
        dataIndex: 'userId',
    },
    {
        title: '邀请人',
        dataIndex: 'createUserId',
    },
    {
        title: '创建时间',
        dataIndex: 'createTime',
    },
    {
        title: '操作',
        key: 'action',
    },
]

// #region 成员数据
// 定义属性
interface Props {
    id: string
}

const props = defineProps<Props>()

// 数据
const dataList = ref<API.SpaceUserVO[]>([])

// 获取数据
const fetchData = async () => {
    const spaceId = props.id
    if (!spaceId) {
        return
    }
    const res = await listSpaceUserUsingPost({
        spaceId,
    })
    if (res.data.data) {
        dataList.value = res.data.data ?? []
    } else {
        message.error('获取数据失败，' + res.data.message)
    }
}

// 页面加载时请求一次
onMounted(() => {
    fetchData()
})
// #endregion

// #region 编辑空间角色
const editSpaceRole = async (value, record) => {
    const res = await editSpaceUserUsingPost({
        id: record.id,
        spaceRole: value,
    })
    if (res.data.code === 0) {
        message.success('修改成功')
    } else {
        message.error('修改失败，' + res.data.message)
    }
}

// #endregion

// #region 删除成员
const doDelete = async (id: string) => {
    if (!id) {
        return
    }
    const res = await deleteSpaceUserUsingPost({ id })
    if (res.data.code === 0) {
        message.success('删除成功')
        // 刷新数据
        fetchData()
    } else {
        message.error('删除失败, ' + res.data.message)
    }
}

// #endregion

// #region 表单数据
// 添加用户
const formData = reactive<API.SpaceUserAddRequest>({})

const handleSubmit = async () => {
    const spaceId = props.id
    if (!spaceId) {
        return
    }
    const res = await addSpaceUserUsingPost({
        spaceId,
        ...formData,
    })
    if (res.data.code === 0) {
        message.success('添加成功')
        // 刷新数据
        fetchData()
    } else {
        message.error('添加失败，' + res.data.message)
    }
}


// #endregion



</script>

<style scoped></style>