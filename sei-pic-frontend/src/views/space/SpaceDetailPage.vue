<template>
    <!-- 搜索表单 -->
    <picture-search-form :onSearch="onSearch" />
    <!-- 空间信息 -->
    <a-flex justify="space-between">
        <h2>{{ space.spaceName }}（私有空间）</h2>
        <a-space size="middle">
            <a-button type="primary" @click="openCreateModal">
                + 创建图片
            </a-button>
            <a-tooltip :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`">
                <a-progress type="circle" :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)" :size="42" />
            </a-tooltip>
        </a-space>
    </a-flex>

    <div style="margin-bottom: 16px;" />

    <!-- 图片列表 -->
    <picture-list :dataList="dataList" :loading="loading" :showOp="true" :canEdit="true" :canDelete="true"
        :onReload="fetchData" />
    <a-pagination style="text-align: right" v-model:current="searchParams.current"
        v-model:pageSize="searchParams.pageSize" :total="total" :show-total="() => `图片总数 ${total} / ${space.maxCount}`"
        @change="onPageChange" />

    <!-- 创建图片弹窗 -->
    <a-modal v-model:open="isModalOpen" title="创建图片" :footer="null" width="720px" @cancel="handleCancel">
        <div v-if="isModalOpen">
            <!-- 选择上传方式 -->
            <a-tabs v-model:activeKey="uploadType">
                <a-tab-pane key="file" tab="文件上传">
                    <PictureUpload :picture="currentPicture" :spaceId="props.id" :onSuccess="onSuccess" />
                </a-tab-pane>
                <a-tab-pane key="url" tab="URL 上传" force-render>
                    <UrlPictureUpload :picture="currentPicture" :spaceId="props.id" :onSuccess="onSuccess" />
                </a-tab-pane>
            </a-tabs>

            <!-- 图片信息表单 -->
            <a-form v-if="currentPicture" name="pictureForm" layout="horizontal" :model="pictureForm"
                @finish="handleSubmit">
                <a-form-item label="名称: " name="name">
                    <a-input v-model:value="pictureForm.name" placeholder="图片名称" />
                </a-form-item>
                <!-- intro -->
                <a-form-item label="简介: " name="introduction">
                    <a-textarea v-model:value="pictureForm.introduction" placeholder="图片简介"
                        :autoSize="{ minRows: 2, maxRows: 5 }" />
                </a-form-item>
                <!-- category -->
                <a-form-item label="分类: " name="category">
                    <a-select v-model:value="pictureForm.category" placeholder="请选择分类" :options="categoryOptions"
                        allow-clear />
                </a-form-item>
                <!-- tag -->
                <a-form-item label="标签: " name="tags">
                    <a-select v-model:value="pictureForm.tags" mode="tags" placeholder="请选择标签" :options="tagOptions"
                        allow-clear />
                </a-form-item>
                <!-- 创建 -->
                <a-form-item>
                    <a-button type="primary" style="width: 100%;" html-type="submit">创建</a-button>
                </a-form-item>
            </a-form>
        </div>
    </a-modal>


</template>


<script setup lang="ts">
import { getPictureVoPageUsingPost } from '@/api/pictureController';
import PictureList from '@/components/PictureList.vue';
import { getSpaceVoUsingPost } from '@/api/spaceController';
import PictureSearchForm from '@/components/PictureSearchForm.vue';
import { formatSize } from '@/utils';
import { editPictureUsingPost, listPictureTagCategoryUsingGet } from '@/api/pictureController';
import PictureUpload from '@/components/PictureUpload.vue';
import UrlPictureUpload from '@/components/UrlPictureUpload.vue';
import { message } from 'ant-design-vue';
import { onMounted, ref, reactive } from 'vue';


interface Props {
    id: string | number
}
const props = defineProps<{ id: string | number }>()
const space = ref<API.SpaceVO>({})
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)
const searchParams = ref<API.PictureQueryRequest>({
    current: 1,
    pageSize: 16,
    sortField: 'createTime',
    sortOrder: 'descend',
})

// 分页参数
const onPageChange = (page, pageSize) => {
    searchParams.value.current = page
    searchParams.value.pageSize = pageSize
    fetchData()
}

// 弹窗控制
const isModalOpen = ref(false);
const uploadType = ref<'file' | 'url'>('file');
const currentPicture = ref<API.PictureVO>();

// 表单数据
const pictureForm = reactive<API.PictureEditRequest>({});

// 标签和分类选项
const tagOptions = ref<any[]>([]);
const categoryOptions = ref<any[]>([]);



/**
 * 获取空间详情
 */
async function fetchSpaceDetail() {
    try {
        const res = await getSpaceVoUsingPost()
        if (res.data.code === 0 && res.data.data) {
            space.value = res.data.data
        } else {
            message.error('获取空间详情失败，' + res.data.message)
        }
    } catch (e: any) {
        message.error('获取空间详情失败：' + e.message)
    }
}
onMounted(() => {
    fetchSpaceDetail()
})

/**
 * 覆盖搜索参数, 拉取数据
 * @param newSearchParams 
 */
function onSearch(newSearchParams: API.PictureQueryRequest) {
    searchParams.value = {
        ...searchParams.value,
        ...newSearchParams,
        current: 1
    }
    fetchData()
}

/**
 * 获取数据
 */
async function fetchData() {
    loading.value = true
    const res = await getPictureVoPageUsingPost({
        spaceId: props.id,
        ...searchParams.value,
    })
    if (res.data.data) {
        dataList.value = res.data.data.records ?? []
        total.value = res.data.data.total ?? 0
    } else {
        message.error('获取数据失败，' + res.data.message)
    }
    loading.value = false
}

// 页面加载时请求一次
onMounted(() => {
    fetchData()
})

/**
 * 打开创建图片弹窗
 */
const openCreateModal = () => {
    isModalOpen.value = true;
    // 重置表单
    Object.assign(pictureForm, {});
    currentPicture.value = undefined;
};

/**
 * 关闭弹窗
 */
const handleCancel = () => {
    isModalOpen.value = false;
};

/**
 * 上传成功回调
 */
const onSuccess = (newPicture: API.PictureVO) => {
    currentPicture.value = newPicture;
    pictureForm.name = newPicture.name;
    pictureForm.introduction = newPicture.introduction;
    pictureForm.category = newPicture.category;
    pictureForm.tags = newPicture.tags;
};

/**
 * 提交表单
 */
const handleSubmit = async (values: any) => {
    // 如果是创建模式，currentPicture.value 应该由上传组件填充
    if (!currentPicture.value) {
        message.error("请先上传图片");
        return;
    }

    const res = await editPictureUsingPost({
        id: currentPicture.value.id,
        spaceId: props.id,
        ...values,
        ...pictureForm
    });

    if (res.data.code === 0 && res.data.data) {
        message.success("操作成功");
        handleCancel();
        fetchData(); // 刷新列表
    } else {
        message.error("操作失败，" + res.data.data.message);
    }
};

/**
 * 获取标签和分类选项
 */
const getTagCategoryOptions = async () => {
    const res = await listPictureTagCategoryUsingGet();
    if (res.data.code === 0 && res.data.data) {
        tagOptions.value = (res.data.data.tagList ?? []).map((tag: string) => ({
            value: tag,
            label: tag
        }));
        categoryOptions.value = (res.data.data.categoryList ?? []).map((tag: string) => ({
            value: tag,
            label: tag
        }));
    } else {
        message.error("获取标签分类失败，" + res.data.message);
    }
};

// 在 onMounted 中调用获取选项
onMounted(() => {
    getTagCategoryOptions();
});



</script>