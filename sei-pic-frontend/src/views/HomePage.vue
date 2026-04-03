<template>
    <div id="homePage">
        <div class="search-section">
            <div class="search-bar">
                <a-input-search v-model:value="searchParams.searchText" placeholder="从海量图片中搜索" enter-button="搜索"
                    size="large" @search="doSearch" />
                <div class="advanced-filter-btn">
                    <a-button type="link" @click="showAdvanced = !showAdvanced">
                        {{ showAdvanced ? '收起筛选' : '高级筛选' }}
                        <template #icon>
                            <component :is="showAdvanced ? 'UpOutlined' : 'DownOutlined'" />
                        </template>
                    </a-button>
                </div>
            </div>
        </div>

        <div v-show="showAdvanced" class="advanced-search-panel">
            <PictureSearchForm :onSearch="doAdvancedSearch" />
        </div>

        <a-tabs v-model:active-key="selectedCategory" @change="doSearch">
            <a-tab-pane key="all" tab="全部" />
            <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
        </a-tabs>

        <div class="tag-bar">
            <span style="margin-right: 8px">标签：</span>
            <a-space :size="[0, 8]" wrap>
                <a-checkable-tag v-for="tag in tagList" :key="tag" :checked="searchParams.tags?.includes(tag)"
                    @change="(checked) => handleTagChange(tag, checked)">
                    {{ tag }}
                </a-checkable-tag>
            </a-space>
        </div>

        <PictureList :dataList="dataList" :loading="loading" />

        <a-pagination style="text-align: right; margin-top: 20px" v-model:current="searchParams.current"
            :page-size-options="['5', '10', '20', '30']" v-model:pageSize="searchParams.pageSize" :total="total"
            @change="onPageChange" />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { UpOutlined, DownOutlined } from '@ant-design/icons-vue'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { getPictureVoPageWithCacheUsingPost, listPictureTagCategoryUsingGet } from '@/api/pictureController'
import { PIC_REVIEW_STATUS_ENUM } from '@/constants/picture'


const handleTagChange = (tag: string, checked: boolean) => {
    // 确保 tags 是个数组
    const currentTags = [...(searchParams.tags ?? [])];

    if (checked) {
        currentTags.push(tag);
    } else {
        const index = currentTags.indexOf(tag);
        if (index > -1) currentTags.splice(index, 1);
    }

    // 更新到响应式对象中
    searchParams.tags = currentTags;
    doSearch();
};




// 控制高级搜索显示
const showAdvanced = ref(false)

// 数据定义
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
    current: 1,
    pageSize: 18,
    sortField: 'createTime',
    sortOrder: 'descend',
    reviewStatus: PIC_REVIEW_STATUS_ENUM.PASS
})

// 获取数据逻辑
const fetchData = async () => {
    loading.value = true
    const tagsArray = tagList.value.filter((_, index) => selectedTagList.value[index])
    const params: API.PictureQueryRequest = { ...searchParams }

    if (selectedCategory.value !== 'all' && selectedCategory.value !== '') {
        params.category = selectedCategory.value;
    }

    try {
        const res = await getPictureVoPageWithCacheUsingPost(params)
        if (res.data.code === 0 && res.data.data) {
            dataList.value = res.data.data.records ?? []
            total.value = res.data.data.total ?? 0
        } else {
            message.error('获取数据失败，' + res.data.message)
        }
    } catch (e: any) {
        message.error('网络请求失败，' + e.message)
    } finally {
        loading.value = false
    }
}

// 响应高级搜索组件的回调
const doAdvancedSearch = (advancedParams: API.PictureQueryRequest) => {
    // 直接合并所有参数，包括那些不在 tag-bar 里的自定义 tags
    Object.assign(searchParams, advancedParams);
    doSearch();
};

// 通用搜索触发
const doSearch = () => {
    searchParams.current = 1
    fetchData()
}

// 分页切换
const onPageChange = (page: number, pageSize: number) => {
    searchParams.current = page
    searchParams.pageSize = pageSize
    fetchData()
}

// 标签和分类配置
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

const getTagCategoryOptions = async () => {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
        tagList.value = res.data.data.tagList ?? []
        categoryList.value = res.data.data.categoryList ?? []
    } else {
        message.error('获取标签分类列表失败，' + res.data.message)
    }
}

onMounted(async () => {
    await getTagCategoryOptions()
    fetchData()
})
</script>

<style scoped>
#homePage {
    margin-bottom: 16px;
}

/* 搜索区域外层，负责居中 */
.search-section {
    display: flex;
    justify-content: center;
    margin-bottom: 16px;
}

/* 搜索条内部，负责单行排列 */
.search-bar {
    display: flex;
    align-items: center;
    width: 100%;
    max-width: 600px;
    /* 适当调大宽度以容纳按钮 */
}

/* 输入框占据主要宽度 */
.search-bar :deep(.ant-input-search) {
    flex: 1;
}

/* 高级筛选按钮样式，紧跟在搜索框后面 */
.advanced-filter-btn {
    margin-left: 8px;
    white-space: nowrap;
    /* 防止文字换行 */
}

.advanced-search-panel {
    background: #fafafa;
    padding: 24px;
    border-radius: 8px;
    border: 1px solid #f0f0f0;
    margin-bottom: 24px;
}

.tag-bar {
    margin-bottom: 16px;
}
</style>