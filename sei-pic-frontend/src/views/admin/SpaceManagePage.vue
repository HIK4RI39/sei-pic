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
                    <a-tag
                        :color="record.spaceLevel === SPACE_LEVEL_ENUM.COMMON ? 'blue' : record.spaceLevel === SPACE_LEVEL_ENUM.PROFESSIONAL ? 'gold' : 'purple'">
                        {{ SPACE_LEVEL_MAP[record.spaceLevel] }}</a-tag>
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
                        <a-button type="link" @click="showUpdateModal(record)">修改</a-button>
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

    <!-- 修改空间信息 -->
    <a-modal v-model:open="isModalVisible" title="修改空间信息" @ok="handleUpdate" @cancel="isModalVisible = false"
        width="600px">
        <a-form :model="formState" layout="vertical">
            <a-form-item label="空间名称" name="spaceName">
                <a-input v-model:value="formState.spaceName" placeholder="请输入空间名称" />
            </a-form-item>
            <!--  -->
            <a-form-item label="空间级别" name="spaceLevel">
                <a-select v-model:value="formState.spaceLevel" placeholder="请选择级别">
                    <a-select-option v-for="opt in SPACE_LEVEL_OPTIONS" :key="opt.value" :value="opt.value">
                        <a-tag :color="opt.value === 0 ? 'blue' : opt.value === 1 ? 'gold' : 'purple'">
                            {{ opt.label }}
                        </a-tag>
                    </a-select-option>
                </a-select>
            </a-form-item>

            <a-row :gutter="16">
                <a-col :span="12">
                    <a-form-item label="最大图片数量" name="maxCount">
                        <a-input-number v-model:value="formState.maxCount" style="width: 100%" :min="1" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="最大图片容量 (MB)" name="maxSize">
                        <a-input-number v-model:value="formState.maxSizeMB" style="width: 100%" :min="1" />
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>
    </a-modal>

</template>

<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
    deleteSpaceUsingPost,
    getSpacePageUsingPost,
    updateSpaceUsingPost
} from '@/api/spaceController'; //
import { SPACE_LEVEL_ENUM, SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS, SPACE_TYPE_MAP, SPACE_TYPE_OPTIONS } from '@/constants/space'; //
import { reactive } from 'vue';

// #region 编辑控件
// 1. 定义前端对应的后端级别数值映射（用于自动更新）
const SPACE_LEVEL_CONFIG = {
    0: { maxCount: 100, maxSize: 100 * 1024 * 1024 }, // 普通
    1: { maxCount: 1000, maxSize: 1000 * 1024 * 1024 }, // 专业
    2: { maxCount: 10000, maxSize: 10000 * 1024 * 1024 }, // 旗舰
};

// 2. 弹窗相关状态
const isModalVisible = ref(false);
const oldSpaceRecord = ref<API.SpaceVO | null>(null); // 存储原始快照
const formState = reactive<any>({
    id: undefined,
    spaceName: '',
    spaceLevel: undefined,
    maxCount: 0,
    maxSizeMB: 0,
});

/**
 * 打开修改弹窗并初始化数据
 */
const showUpdateModal = (record: API.SpaceVO) => {
    oldSpaceRecord.value = { ...record }; // 深拷贝一份初始数据
    formState.id = record.id;
    formState.spaceName = record.spaceName;
    formState.spaceLevel = record.spaceLevel;
    formState.maxCount = record.maxCount;
    formState.maxSizeMB = record.maxSize ? Math.floor(record.maxSize / (1024 * 1024)) : 0;
    isModalVisible.value = true;
};

/**
 * 监听空间级别切换，自动升级限额
 */
watch(() => formState.spaceLevel, (newLevel: number) => {
    if (newLevel === undefined || !oldSpaceRecord.value) return;

    const config = SPACE_LEVEL_CONFIG[newLevel];
    const oldRecord = oldSpaceRecord.value;

    if (config) {
        // 逻辑：如果新级别的预设值 >= 空间原始值，说明处于“标准或升级”路径，自动跟随预设
        // 这样从旗舰版调回普通版时，由于 100 < 10000，它会乖乖调回 100
        const configSizeMB = config.maxSize / (1024 * 1024);
        const oldSizeMB = (oldRecord.maxSize || 0) / (1024 * 1024);

        // 更新数量
        if (config.maxCount !== oldRecord.maxCount) {
            formState.maxCount = config.maxCount;
        }

        // 更新容量
        if (configSizeMB !== oldSizeMB) {
            formState.maxSizeMB = configSizeMB;
        }
    }
});

/**
 * 提交修改
 */
const handleUpdate = async () => {
    try {
        const res = await updateSpaceUsingPost({
            id: formState.id,
            spaceName: formState.spaceName,
            spaceLevel: formState.spaceLevel,
            maxCount: formState.maxCount,
            // MB 转字节发送给后端
            maxSize: formState.maxSizeMB * 1024 * 1024,
        });
        if (res.data.code === 0) {
            message.success('修改成功');
            isModalVisible.value = false;
            fetchData();
        } else {
            message.error('修改失败：' + res.data.message);
        }
    } catch (e: any) {
        message.error('修改异常：' + e.message);
    }
};

// #endregion


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