<template>
    <div id="myPicturePage">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
            <h2>我的上传记录</h2>
            <a-button type="primary" href="/picture/add" target="_blank">+ 上传新图片</a-button>
        </div>

        <a-card style="margin-bottom: 16px">
            <a-form :model="searchParams" layout="inline" @finish="doSearch">
                <a-form-item label="名称" name="name">
                    <a-input v-group v-model:value="searchParams.name" placeholder="搜索图片名" allow-clear />
                </a-form-item>
                <a-form-item>
                    <a-space>
                        <a-button type="primary" html-type="submit">查询</a-button>
                        <a-button @click="resetSearchParams">重置</a-button>
                    </a-space>
                </a-form-item>
            </a-form>
        </a-card>

        <a-table row-key="id" :columns="columns" :data-source="dataList" :loading="loading" :pagination="false">
            <template #bodyCell="{ column, record }">
                <!-- url -->
                <template v-if="column.dataIndex === 'url'">
                    <a-image :src="record.url" :width="60" style="border-radius: 4px" />
                </template>

                <template v-else-if="column.dataIndex === 'name'">
                    <!-- <span @click="router.push(`/picture/${record.id}`)"> -->
                    <span @click="showDetail(record.id)">
                        {{ record.name }}
                    </span>
                </template>



                <!-- 审核状态 -->
                <template v-else-if="column.dataIndex === 'reviewStatus'">
                    <a-tag :color="getReviewStatusColor(record.reviewStatus)">
                        {{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}
                    </a-tag>
                    <div v-if="record.reviewStatus === 2" style="font-size: 12px; color: #ff4d4f; margin-top: 4px">
                        原因: {{ record.reviewMessage }}
                    </div>
                </template>
                <!-- 上传时间 -->
                <template v-else-if="column.dataIndex === 'createTime'">
                    <span> {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </span>
                </template>
                <!-- 更新时间 -->
                <template v-else-if="column.dataIndex === 'updateTime'">
                    <span> {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </span>
                </template>

                <template v-else-if="column.key === 'action'">
                    <a-space wrap>
                        <a-button type="link" size="small" @click="doUpdate(record.id)">编辑</a-button>

                        <a-button type="link" size="small" @click="doSetAvatar(record.url)">设为头像</a-button>

                        <a-popconfirm title="确定要删除这张图片吗？" ok-text="确定" cancel-text="取消" @confirm="doDelete(record.id)">
                            <a-button type="link" danger size="small">删除</a-button>
                        </a-popconfirm>
                    </a-space>
                </template>
            </template>
        </a-table>
        <!-- 分页条 -->
        <a-pagination style="margin-top: 20px; text-align: right" v-model:current="searchParams.current"
            v-model:pageSize="searchParams.pageSize" :total="total" @change="fetchData" show-size-changer />
        <!-- 图片详情弹窗 -->
        <picture-detail-modal v-model:visible="detailVisible" :id="currentId" />
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { PIC_REVIEW_STATUS_MAP } from '@/constants/picture'
import { useLoginUserStore } from '@/stores/useLoginStore'
import { deletePictureByIdUsingPost, getPictureVoByIdUsingPost, getPictureVoPageUsingPost, listPictureTagCategoryUsingGet } from '@/api/pictureController'
import dayjs from 'dayjs'
import { editUserUsingPost } from '@/api/userController'
import PictureDetailModal from '@/components/picture/PictureDetailModal.vue'

// #region 图片详情弹窗
const detailVisible = ref(false);
const currentId = ref<string | number>('');
// 点击图片时触发
const showDetail = (id: string | number) => {
    currentId.value = id;
    detailVisible.value = true;
};
// #endregion

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(false)
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const categoryOptions = ref([])

// 搜索参数，初始带上当前登录用户的 ID
const searchParams = reactive<API.PictureQueryRequest>({
    current: 1,
    pageSize: 10,
    sortField: 'createTime',
    sortOrder: 'descend',
    userId: loginUserStore.loginUser.id // 关键：只查自己的
})

const columns = [
    { title: '图片', dataIndex: 'url', width: 100 },
    { title: '名称', dataIndex: 'name', ellipsis: true },
    { title: '审核状态', dataIndex: 'reviewStatus', width: 150 },
    { title: '上传时间', dataIndex: 'createTime', width: 180 },
    { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

/**
 * 加载数据
 */
const fetchData = async () => {
    loading.value = true
    try {
        const res = await getPictureVoPageUsingPost({ ...searchParams })
        if (res.data.code === 0) {
            dataList.value = res.data.data?.records ?? []
            total.value = res.data.data?.total ?? 0
        }
    } finally {
        loading.value = false
    }
}

/**
 * 设为头像
 */
const doSetAvatar = async (url: string) => {
    Modal.confirm({
        title: '确认更换头像',
        content: '确定要将选中的图片设为您的个人头像吗？',
        async onOk() {
            try {
                const res = await editUserUsingPost({
                    userAvatar: url
                })
                if (res.data.code === 0) {
                    message.success('头像设置成功')
                    // 更新本地 store 中的头像显示
                    loginUserStore.setLoginUser({ ...loginUserStore.loginUser, userAvatar: url })
                } else {
                    message.error('设置失败：' + res.data.message)
                }
            } catch (e) {
                message.error('网络异常')
            }
        }
    })
}

/**
 * 状态颜色映射
 */
const getReviewStatusColor = (status: number) => {
    if (status === 0) return 'orange'
    if (status === 1) return 'green'
    return 'red'
}

const doSearch = () => {
    searchParams.current = 1
    fetchData()
}

const resetSearchParams = () => {
    Object.assign(searchParams, {
        current: 1,
        name: '',
        category: '',
        userId: loginUserStore.loginUser.id
    })
    fetchData()
}

const doUpdate = (id: number) => {
    router.push(`/picture/add?id=${id}`)
}

const doDelete = async (id: number) => {
    const res = await deletePictureByIdUsingPost({ id })
    if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
    }
}

const loadOptions = async () => {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0) {
        categoryOptions.value = (res.data.data?.categoryList ?? []).map(c => ({ label: c, value: c }))
    }
}

onMounted(() => {
    fetchData()
    loadOptions()
})
</script>

<style scoped>
#myPicturePage {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}
</style>