<template>
    <div id="spaceManagePage">
        <h2>空间管理</h2>
        <a-form :model="searchParams" layout="inline" autocomplete="off" @finish="doSearch">
            <a-form-item label="空间名称" name="spaceName">
                <a-input v-model:value="searchParams.spaceName" placeholder="输入空间名称" allow-clear />
            </a-form-item>
            <a-form-item label="空间级别" name="spaceLevel">
                <a-select v-model:value="searchParams.spaceLevel" style="min-width: 120px" placeholder="选择级别"
                    allow-clear :options="SPACE_LEVEL_OPTIONS" />
            </a-form-item>
            <!-- 空间类型 -->
            <a-form-item label="空间类别" name="spaceType">
                <a-select v-model:value="searchParams.spaceType" :options="SPACE_TYPE_OPTIONS" placeholder="请输入空间类型"
                    style="min-width: 180px" allow-clear />
            </a-form-item>

            <a-form-item>
                <a-button type="primary" html-type="submit">搜索</a-button>
            </a-form-item>

            <a-space>
                <a-button type="primary" href="/space/create" target="_blank">+ 创建空间</a-button>
                <a-button type="primary" ghost href="/space_analyze?queryPublic=1" target="_blank">
                    分析公共图库
                </a-button>
                <a-button type="primary" ghost href="/space_analyze?queryAll=1" target="_blank">
                    分析全空间
                </a-button>
            </a-space>


        </a-form>

        <div style="margin-bottom: 16px" />

        <a-table :columns="columns" :data-source="dataList" :pagination="false">
            <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'spaceLevel'">
                    <a-tag color="blue">{{ SPACE_LEVEL_MAP[record.spaceLevel] }}</a-tag>
                </template>
                <!-- 空间类别 -->
                <template v-else-if="column.key === 'spaceType'">
                    <a-tag>{{ SPACE_TYPE_MAP[record.spaceType] }}</a-tag>
                </template>
                <template v-else-if="column.key === 'usage'">
                    <div>数量：{{ record.totalCount }} / {{ record.maxCount }}</div>
                    <div>大小：{{ (record.totalSize / 1024 / 1024).toFixed(2) }} MB / {{ (record.maxSize / 1024 /
                        1024).toFixed(2) }} MB</div>
                </template>
                <template v-else-if="column.key === 'createTime'">
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                </template>
                <template v-else-if="column.key === 'action'">
                    <a-space>
                        <a-button type="link" :href="`/space_analyze?spaceId=${record.id}`"
                            target="_blank">分析</a-button>
                        <a-button type="link" :href="`/add_space?id=${record.id}`">修改</a-button>
                        <a-button danger type="link" @click="doDelete(record.id)">删除</a-button>
                    </a-space>
                </template>
            </template>
        </a-table>

        <div class="table-pagination">
            <a-pagination v-model:current="searchParams.current" v-model:page-size="searchParams.pageSize"
                :total="total" :pageSizeOptions="['5', '10', '20']" show-size-changer @change="handlePageChange" />
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
    deleteSpaceUsingPost,
    getSpacePageUsingPost
} from '@/api/spaceController'; //
import { SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS, SPACE_TYPE_MAP, SPACE_TYPE_OPTIONS } from '@/constants/space'; //

// 表格列定义
const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: '空间名称', dataIndex: 'spaceName', key: 'spaceName' },
    { title: '空间级别', dataIndex: 'spaceLevel', key: 'spaceLevel' },
    { title: '空间类别', dataIndex: 'spaceType', key: "spaceType" },
    { title: '使用情况', key: 'usage' },
    { title: '创建人ID', dataIndex: 'userId', key: 'userId' },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
    { title: '操作', key: 'action' },
];

const dataList = ref<API.SpaceVO[]>([]);
const total = ref<number>(0);

// 搜索参数，对应 API.SpaceQueryRequest
const searchParams = ref<any>({
    current: 1,
    pageSize: 10,
    spaceName: '',
    spaceLevel: undefined,
});

/**
 * 加载数据
 */
const fetchData = async () => {
    try {
        const res = await getSpacePageUsingPost({ ...searchParams.value }); //
        if (res.data.code === 0 && res.data.data) {
            dataList.value = res.data.data.records || [];
            total.value = res.data.data.total || 0;
        } else {
            message.error('加载失败：' + res.data.message);
        }
    } catch (e: any) {
        message.error('加载异常：' + e.message);
    }
};

const doSearch = () => {
    searchParams.value.current = 1;
    fetchData();
};

const handlePageChange = (page: number, pageSize: number) => {
    searchParams.value.current = page;
    searchParams.value.pageSize = pageSize;
    fetchData();
};

const doDelete = async (id: number) => {
    if (!confirm('确定删除该空间吗？此操作不可恢复')) return;
    try {
        const res = await deleteSpaceUsingPost({ id }); //
        if (res.data.code === 0) {
            message.success('删除成功');
            fetchData();
        } else {
            message.error('删除失败：' + res.data.message);
        }
    } catch (e: any) {
        message.error('删除异常：' + e.message);
    }
};

onMounted(() => {
    fetchData();
});
</script>

<style scoped>
.table-pagination {
    margin-top: 16px;
    text-align: right;
}
</style>