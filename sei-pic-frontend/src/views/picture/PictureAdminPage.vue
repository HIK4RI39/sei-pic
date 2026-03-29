<template>
    <div id="pictureAdminPage">
        <!-- 搜索表单 -->
        <a-form :model="searchParams" layout="horizontal" @finish="doSearch">
            <a-row :gutter="16">
                <a-col :span="4">
                    <a-form-item label="名称" name="name">
                        <a-input v-model:value="searchParams.name" allow-clear />
                    </a-form-item>
                </a-col>
                <a-col :span="5">
                    <a-form-item label="简介" name="introduction">
                        <a-input v-model:value="searchParams.introduction" allow-clear />
                    </a-form-item>
                </a-col>
                <a-col :span="5">
                    <a-form-item label="关键词" name="searchText">
                        <a-input v-model:value="searchParams.searchText" allow-clear />
                    </a-form-item>
                </a-col>
                <a-col :span="3">
                    <a-form-item label="分类" name="category">
                        <a-select v-model:value="searchParams.category" :options="categoryOptions" allow-clear />
                    </a-form-item>
                </a-col>
                <a-col :span="4">
                    <a-form-item label="审核状态" name="reviewStatus">
                        <a-select v-model:value="searchParams.reviewStatus" :options="[
                            { 'label': '待审核', 'value': 0 },
                            { 'label': '通过', 'value': 1 },
                            { 'label': '拒绝', 'value': 2 },
                        ]" allow-clear />
                    </a-form-item>
                </a-col>

            </a-row>

            <a-row :gutter="16">
                <a-col :span="7">
                    <a-form-item label="标签" name="tags">
                        <a-select v-model:value="searchParams.tags" mode="tags" :options="tagOptions" allow-clear />
                    </a-form-item>
                </a-col>
                <a-col :span="3">
                    <a-form-item label="格式" name="picFormat">
                        <a-select v-model:value="searchParams.picFormat" :options="formatOptions" allow-clear />
                    </a-form-item>
                </a-col>
                <a-col :span="4">
                    <a-form-item label="排序字段" name="sortOrder">
                        <a-select v-model:value="searchParams.sortField" :options="sortOptions" allow-clear />
                    </a-form-item>
                </a-col>
                <a-col :span="4">
                    <a-form-item label="排序顺序" name="sortOrder">
                        <a-select v-model:value="searchParams.sortOrder" allow-clear>
                            <a-select-option value="ascend">升序</a-select-option>
                            <a-select-option value="descend">降序</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
                <a-col :span="6" style="text-align: right">
                    <a-space>
                        <a-button type="primary" html-type="submit">搜索</a-button>
                        <a-button @click="() => { searchParams = {} }">重置</a-button>
                    </a-space>
                </a-col>
            </a-row>
        </a-form>


        <div style="margin-bottom: 16px"></div>

        <a-table :columns="columns" :data-source="dataList" :pagination="false" :scroll="{ x: 1600 }">
            <!-- 表头 -->
            <template #headerCell="{ column }">
                {{ column.title }}
            </template>
            <!-- 单元格 -->
            <template #bodyCell="{ column, record }">
                <!-- id -->
                <template v-if="column.dataIndex === 'id'">
                    {{ record.id }}
                </template>
                <!-- url -->
                <template v-else-if="column.dataIndex === 'url'">
                    <a-image :src="record.url" :width="50" />
                </template>
                <!-- name -->
                <template v-else-if="column.dataIndex === 'name'">
                    <a-tooltip placement="topLeft">
                        <template #title>{{ record.name }}</template>
                        <router-link :to="`/picture/${record.id}`"
                            style="display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                            {{ record.name }}
                        </router-link>
                    </a-tooltip>
                </template>
                <!-- intro -->
                <template v-else-if="column.dataIndex === 'introduction'">
                    {{ record.introduction }}
                </template>
                <!-- category -->
                <template v-else-if="column.dataIndex === 'category'">
                    <span>
                        <a-tag>
                            {{ record.category }}
                        </a-tag>
                    </span>
                </template>
                <!-- tags -->
                <!-- <template v-if="column.dataIndex === 'tags'">
                    <a-space wrap>
                        <template v-if="record.tags && record.tags !== '[]' && record.tags !== ''">
                            <a-tag v-for="tag in safeParseTags(record.tags)" :key="tag">
                                {{ tag }}
                            </a-tag>
                        </template>
                        <span v-else>-</span>
                    </a-space>
                </template> -->
                <template v-if="column.dataIndex === 'tags'">
                    <a-space wrap :size="[0, 2]">
                        <a-tag v-for="tag in safeParseTags(record.tags)" :key="tag" style="font-size: 11px; margin: 0">
                            {{ tag }}
                        </a-tag>
                    </a-space>
                </template>

                <!-- picInfo -->
                <!-- <template v-if="column.dataIndex === 'picInfo'">
                    <span>
                        <div>格式：{{ record.picFormat }}</div>
                        <div>宽度：{{ record.picWidth }}</div>
                        <div>高度：{{ record.picHeight }}</div>
                        <div>宽高比：{{ record.picScale }}</div>
                        <div>大小：{{ (record.picSize / 1024).toFixed(2) }}KB</div>
                    </span>
                </template> -->
                <template v-if="column.dataIndex === 'picInfo'">
                    <div class="pic-info-text">
                        <div>{{ record.picFormat }} · {{ (record.picSize / 1024).toFixed(1) }}KB</div>
                        <div>{{ record.picWidth }}x{{ record.picHeight }} ({{ record.picScale }})</div>
                    </div>
                </template>
                <!-- 用户id -->
                <template v-else-if="column.dataIndex === 'userId'">
                    {{ record.userId }}
                </template>
                <!-- 审核信息 -->
                <template v-else-if="column.dataIndex === 'reviewMessage'">
                    <span class="review-info-text">
                        <div>审核状态：
                            <!-- {{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }} -->
                            <a-tag v-if="PIC_REVIEW_STATUS_ENUM.REVIEWING === record.reviewStatus" color="blue"
                                style="font-size: 8px;" :bordered="false">待审核</a-tag>
                            <a-tag v-else-if="PIC_REVIEW_STATUS_ENUM.PASS === record.reviewStatus"
                                style="font-size: 8px;" :bordered="false" color="green">通过</a-tag>
                            <a-tag v-else-if="PIC_REVIEW_STATUS_ENUM.REJECT === record.reviewStatus"
                                style="font-size: 8px;" :bordered="false" color="red">拒绝</a-tag>
                        </div>
                        <div>审核信息：{{ record.reviewMessage }}</div>
                        <div>审核人id：{{ record.reviewerId }}</div>
                    </span>
                </template>
                <!-- createTime -->
                <template v-else-if="column.dataIndex === 'createTime'">
                    <span>
                        {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </span>
                </template>
                <!-- editTime -->
                <template v-else-if="column.dataIndex === 'editTime'">
                    <span>
                        {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </span>
                </template>
                <!-- action -->
                <template v-else-if="column.key === 'action'">
                    <span>
                        <a-row>
                            <a-space>

                                <!-- 更新 -->
                                <a-col>
                                    <a-button type="primary" ghost size="small" style="font-size: 10px;"
                                        @click="doUpdate(record.id)">更新</a-button>
                                </a-col>
                                <!-- 删除 -->
                                <a-col>
                                    <a-button danger @click="doDelete(record.id)" size="small"
                                        style="font-size: 10px;">删除</a-button>
                                </a-col>
                                <!-- 通过 -->
                                <a-col>
                                    <a-button type="primary" size="small" style="font-size: 10px;"
                                        v-if="PIC_REVIEW_STATUS_ENUM.PASS !== record.reviewStatus"
                                        @click="reviewPass(record.id)">通过</a-button>
                                </a-col>
                                <!-- 拒绝 -->
                                <a-col>
                                    <a-button type="primary" danger size="small" style="font-size: 10px;"
                                        v-if="PIC_REVIEW_STATUS_ENUM.REJECT !== record.reviewStatus"
                                        @click="reviewReject(record.id)">拒绝</a-button>
                                </a-col>
                            </a-space>

                        </a-row>
                    </span>
                </template>
            </template>
        </a-table>

        <a-pagination id="pagination" v-model="searchParams.current" :total="total"
            v-model:page-size="searchParams.pageSize" :pageSizeOptions="['5', '10', '15', '20', '30']" show-size-changer
            show-quick-jumper @change="handlePageChange" />

    </div>
</template>

<script lang="ts" setup>
import { deletePictureByIdUsingPost, getPicturePageUsingPost, listPictureTagCategoryUsingGet, reviewPictureUsingPost } from '@/api/pictureController';
import { PIC_REVIEW_STATUS_ENUM, PIC_REVIEW_STATUS_MAP } from '@/constants/picture';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed } from 'vue';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter()


const columns = [
    {
        title: 'id',
        dataIndex: 'id',
        width: 180
    },
    {
        title: '图片',
        dataIndex: 'url',
        width: 120
    },
    {
        title: '名称',
        dataIndex: 'name',
        width: 120,
        ellipsis: true
    },
    {
        title: '简介',
        dataIndex: 'introduction',
        width: 120,
        ellipsis: true,
    },
    {
        title: '类型',
        dataIndex: 'category',
        width: 100,
    },
    {
        title: '标签',
        dataIndex: 'tags',
        width: 100,
    },

    {
        title: '审核信息',
        dataIndex: 'reviewMessage',
        width: 180,
        ellipsis: true
    },
    {
        title: '图片信息',
        dataIndex: 'picInfo',
        width: 120
    },
    {
        title: '用户 id',
        dataIndex: 'userId',
        width: 180
    },
    {
        title: '创建时间',
        dataIndex: 'createTime',
        width: 180
    },
    {
        title: '编辑时间',
        dataIndex: 'editTime',
        width: 180
    },
    {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 220,
    },
]


const dataList = ref<API.PictureVO[]>([])
/**
 * 搜索参数
 */
const searchParams = ref<API.PictureQueryRequest>({
    current: 1,
    pageSize: 8,
    sortOrder: 'descend'
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


/**
 * 获取数据
 */
const fetchData = async () => {
    const res = await getPicturePageUsingPost({
        ...searchParams.value
    })
    try {
        if (res.data.code === 0 && res.data.data) {
            // console.log("数据: ", res.data.data)
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

const doUpdate = (id: number) => {
    router.push('/picture/add?id=' + id)
}


const doDelete = async (id: number) => {
    const result = confirm("确认删除?")
    if (!result) {
        return
    }

    const res = await deletePictureByIdUsingPost({ id })
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

const reviewPass = async (id: number) => {
    const res = await reviewPictureUsingPost({
        id,
        reviewStatus: PIC_REVIEW_STATUS_ENUM.PASS,
        reviewMessage: '管理员操作通过'
    })
    if (res.data.code === 0) {
        message.success("已通过")
        fetchData()
    } else {
        message.error("审核失败," + res.data.message)
    }
}

const reviewReject = async (id: number) => {
    const msg = prompt("拒绝原因: ")

    const res = await reviewPictureUsingPost({
        id,
        reviewStatus: PIC_REVIEW_STATUS_ENUM.REJECT,
        reviewMessage: msg ? msg : '管理员操作拒绝'
    })
    if (res.data.code === 0) {
        message.success("已通过")
        fetchData()
    } else {
        message.error("审核失败," + res.data.message)
    }
}

/**
 * 安全解析标签字符串
 * @param tagsJson 
 */
const safeParseTags = (tagsJson: string | string[]) => {
    if (!tagsJson) return [];
    // 如果后端已经转成了数组，直接返回
    if (Array.isArray(tagsJson)) return tagsJson;
    try {
        return JSON.parse(tagsJson);
    } catch (e) {
        console.error("标签解析失败:", e);
        return [];
    }
}

const tagOptions = ref<string[] | undefined>([])
const categoryOptions = ref<string[] | undefined>([])
const formatOptions = [
    { value: 'PNG', label: 'PNG' },
    { value: 'JPEG', label: 'JPEG' },
    { value: 'WEBP', label: 'WEBP' }
]
const sortOptions = [
    { value: 'id', label: 'id' },
    { value: 'userId', label: '用户id' },
    { value: 'createTime', label: '创建时间' },
    { value: 'editTime', label: '编辑时间' }
]

/**
 * 获取预设的tag, category列表
 */
const getTagCategoryOptions = async () => {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
        tagOptions.value = (res.data.data.tagList ?? []).map((tag: string) => {
            return {
                value: tag,
                label: tag
            }
        })

        categoryOptions.value = (res.data.data.categoryList ?? []).map((tag: string) => {
            return {
                value: tag,
                label: tag
            }
        })

    } else {
        message.error("获取标签分类失败," + res.data.message)
    }
}

onMounted(() => {
    getTagCategoryOptions()
})

</script>

<style scoped>
/* 1. 极简单元格间距 */
:deep(.ant-table-tbody > tr > td),
:deep(.ant-table-thead > tr > th) {
    padding: 6px 8px !important;
    /* 进一步压缩内边距 */
}

/* 2. 限制行高：通过图片高度控制 */
:deep(.ant-image-img) {
    height: 48px !important;
    /* 压低图片高度 */
    width: 48px !important;
    object-fit: cover;
    border-radius: 4px;
}

/* 3. 紧凑型图片信息 */
.pic-info-text {
    font-size: 11px;
    /* 减小字号 */
    line-height: 1.2;
    color: #666;
}

/* 紧凑型审核信息 */
.review-info-text {
    font-size: 11px;
    line-height: 1.8;
    color: #666;
}

/* 4. 容器限宽并美化滚动条 */
#pictureAdminPage {
    padding: 16px;
    background: #fff;
}

/* 优化滚动条样式（可选，让界面更精致） */
:deep(.ant-table-body)::-webkit-scrollbar {
    height: 8px;
}

:deep(.ant-table-body)::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 4px;
}

#pictureAdminPage {
    max-width: 1440px;
    /* 设置一个标准的最大宽度 */
    margin: 0 auto;
    /* 水平居中 */
    padding: 20px;
    /* 四周留白，避免贴边 */
}

/* 调整分页器间距 */
#pagination {
    margin-top: 20px;
    text-align: center;
}
</style>