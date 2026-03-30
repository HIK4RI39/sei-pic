<template>
    <h2>用户管理</h2>
    <div id="userManagepage">
        <!-- 搜索表单 -->
        <a-form :model="searchParams" name="horizontal_login" layout="inline" autocomplete="off" @finish="doSearch">
            <!-- userAccount -->
            <a-form-item label="账号" name="userAccount">
                <a-input v-model:value="searchParams.userAccount" allow-clear>
                    <template #prefix>
                        <UserOutlined class="site-form-item-icon" />
                    </template>
                </a-input>
            </a-form-item>
            <!-- userName -->
            <a-form-item label="用户名" name="userName">
                <a-input v-model:value="searchParams.userName" allow-clear>z
                    <template #prefix>
                        <LockOutlined class="site-form-item-icon" />
                    </template>
                </a-input>
            </a-form-item>
            <!-- submit -->
            <a-form-item>
                <a-button type="primary" html-type="submit">搜索</a-button>
            </a-form-item>
        </a-form>


        <div style="margin-bottom: 16px" />

        <a-table :columns="columns" :data-source="dataList" :pagination="false">
            <!-- 表头 -->
            <template #headerCell="{ column }">
                {{ column.name }}
            </template>
            <!-- 单元格 -->
            <template #bodyCell="{ column, record }">
                <!-- userAccount -->
                <template v-if="column.key === 'userAccount'">
                    <!-- TODO 点击跳转用户空间 (如果没有 说明未创建) -->
                    {{ record.userAccount }}
                </template>
                <!-- userName -->
                <template v-else-if="column.key === 'userName'">
                    {{ record.userName ? record.userName : '无名' }}
                </template>
                <!-- userAvatar -->
                <template v-else-if="column.key === 'userAvatar'">
                    <a-avatar :src="record.userAvatar" />
                </template>
                <!-- userProfile -->
                <template v-else-if="column.key === 'userProfile'">
                    {{ record.userProfile }}
                </template>
                <!-- userRole -->
                <template v-else-if="column.key === 'userRole'">
                    <span>
                        <a-tag :color="record.userRole === 'admin' ? 'volcano' : 'geekblue'">
                            {{ record.userRole === 'admin' ? '管理员' : '用户' }}
                        </a-tag>
                    </span>
                </template>
                <!-- createTime -->
                <template v-else-if="column.key === 'createTime'">
                    <span>
                        {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </span>
                </template>
                <!-- updateTime -->
                <template v-else-if="column.key === 'updateTime'">
                    <span>
                        {{ dayjs(record.updateTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </span>
                </template>
                <!-- action -->
                <template v-else-if="column.key === 'action'">
                    <span>
                        <a-row>
                            <a-space>
                                <!-- 更新 -->
                                <a-col>
                                    <a-button type="primary" ghost>更新</a-button>
                                </a-col>
                                <!-- 删除 -->
                                <a-col>
                                    <a-button danger @click="doDelete(record.id)">删除</a-button>
                                </a-col>
                            </a-space>
                        </a-row>
                    </span>
                </template>
            </template>
        </a-table>

        <div class="table-pagination">
            <a-pagination v-model="searchParams.current" :total="total" v-model:page-size="searchParams.pageSize"
                :pageSizeOptions="['5', '10', '15', '20', '30']" show-size-changer show-quick-jumper
                @change="handlePageChange" />
        </div>

    </div>
</template>

<script lang="ts" setup>
import { deleteUserByIdUsingPost, listUserVoByPageUsingPost } from '@/api/userController';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed } from 'vue';
import { onMounted, ref } from 'vue';

const columns = [
    {
        'name': 'id',
        'dataIndex': 'id',
        'key': 'id'
    }, {
        'name': '账号',
        'dataIndex': 'userAccount',
        'key': 'userAccount'

    }, {
        'name': '用户名',
        'dataIndex': 'userName',
        'key': 'userName'
    },
    {
        'name': '头像',
        'dataIndex': 'userAvatar',
        'key': 'userAvatar'
    },
    {
        'name': '简介',
        'dataIndex': 'userProfile',
        'key': 'userProfile',
        'ellipsis': true,

    }, {
        'name': '用户角色',
        'dataIndex': 'userRole',
        'key': 'userRole'

    }, {
        'name': '创建时间',
        'dataIndex': 'createTime',
        'key': 'createTime'
    }, {
        'name': '更新时间',
        'dataIndex': 'updateTime',
        'key': 'updateTime'
    }, {
        'name': '操作',
        'dataIndex': 'action',
        'key': 'action'
    },
];

const dataList = ref<API.UserVO[]>([])
/**
 * 搜索参数
 */
const searchParams = ref<API.UserPageRequest>({
    current: 1,
    pageSize: 8
})

const total = ref<Number>(0)

const pagination = computed(() => ({
    current: searchParams.value.current,
    pageSize: searchParams.value.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total.value} 条`
}))

const handlePageChange = (page: number, pageSize: number) => {
    // pageSize变化, current退回第一页
    if (pageSize !== searchParams.value.pageSize) {
        searchParams.value.current = 1
    } else {
        searchParams.value.current = page;
    }
    searchParams.value.pageSize = pageSize;
    fetchData()
}

// const onSizeChange = (current, size) => {
//     searchParams.value.current = size
//     searchParams.value.current = 1
//     fetchData()
// }
// const onChange = (page, pageSize) => {
//     console.log("page: ", page)
//     console.log("pageSize: ", pageSize)
//     searchParams.value.current = page
//     searchParams.value.pageSize = pageSize
//     fetchData()
// }

/**
 * 获取数据
 */
const fetchData = async () => {
    const res = await listUserVoByPageUsingPost({
        ...searchParams.value
    })
    try {
        if (res.data.code === 0 && res.data.data) {
            message.success("获取数据成功")
            dataList.value = res.data.data.records
            total.value = res.data.data.total
        } else {
            message.error("获取数据失败," + res.data.message)
        }
    } catch (e: any) {
        message.error("获取数据失败," + e.message);
    }
}

const doSearch = () => {
    searchParams.value.current = 1
    fetchData()
}

onMounted(() => {
    fetchData()
})

const doDelete = async (id: number) => {
    const result = confirm("确认删除?")
    if (!result) {
        return
    }

    const res = await deleteUserByIdUsingPost({ id })
    try {
        if (res.data.code === 0 && res.data.data) {
            message.success("删除成功")
            // 重新拉取数据
            doSearch()
        } else {
            message.error("删除失败," + res.data.message)
        }
    } catch (e: any) {
        message.error("删除失败," + e.message);
    }
}

</script>

<style scoped>
.table-pagination {
    margin-top: 16px;
    text-align: center;
}
</style>