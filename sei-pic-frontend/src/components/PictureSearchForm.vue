<template>
    <div class="picture-search-form">
        <a-form layout="inline" :model="searchParams" @finish="doSearch" class="search-form-container">
            <a-form-item label="关键词" name="searchText">
                <a-input v-model:value="searchParams.searchText" placeholder="从名称和简介搜索" allow-clear />
            </a-form-item>

            <a-form-item label="分类" name="category">
                <a-auto-complete v-model:value="searchParams.category" style="min-width: 150px"
                    :options="categoryOptions" placeholder="请输入分类" allowClear />
            </a-form-item>

            <a-form-item label="标签" name="tags">
                <a-select v-model:value="searchParams.tags" style="min-width: 150px" :options="tagOptions" mode="tags"
                    placeholder="请输入标签" allowClear />
            </a-form-item>

            <a-form-item label="比例" name="scaleType">
                <a-select v-model:value="searchParams.scaleType" style="min-width: 120px" :options="scaleOptions"
                    placeholder="图片比例" allowClear />
            </a-form-item>

            <a-form-item label="格式" name="picFormat">
                <a-select v-model:value="searchParams.picFormat" style="min-width: 120px" :options="formatOptions"
                    placeholder="格式" allowClear />
            </a-form-item>

            <a-form-item label="编辑日期" name="">
                <a-range-picker style="width: 340px" show-time v-model:value="dateRange" :placeholder="['开始日期', '结束时间']"
                    format="YYYY/MM/DD HH:mm:ss" :presets="rangePresets" @change="onRangeChange" />
            </a-form-item>

            <a-form-item class="buttons-item">
                <a-space>
                    <a-button type="primary" html-type="submit" style="width: 80px">搜索</a-button>
                    <a-button html-type="reset" @click="doClear">重置</a-button>
                </a-space>
            </a-form-item>
        </a-form>
    </div>
</template>


<script setup lang="ts">
import { listPictureTagCategoryUsingGet } from '@/api/pictureController';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { reactive } from 'vue';
import { ref } from 'vue';
import { onMounted } from 'vue';

interface Props {
    onSearch?: (searchParams: API.PictureQueryRequest) => void
}
const props = defineProps<Props>()

const searchParams = reactive<API.PictureQueryRequest>({})
function doSearch() {
    props.onSearch?.(searchParams)
}

const dateRange = ref<[]>([])

/**
 * 日期范围更改时触发
 * @param dates
 * @param dateStrings
 */
const onRangeChange = (dates: any[], dateStrings: string[]) => {
    if (dates.length < 2) {
        searchParams.startEditTime = undefined
        searchParams.endEditTime = undefined
    } else {
        searchParams.startEditTime = dates[0].toDate()
        searchParams.endEditTime = dates[1].toDate()
    }
}


// 预设日期
const rangePresets = ref([
    { label: '过去 7 天', value: [dayjs().add(-7, 'd'), dayjs()] },
    { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
    { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
    { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])


const tagOptions = ref<string[] | undefined>([])
const categoryOptions = ref<string[] | undefined>([])
const formatOptions = [
    { value: 'png', label: 'png' },
    { value: 'jpeg', label: 'jpeg' },
    { value: 'webp', label: 'webp' }
]

const scaleOptions = [
    { value: 'horizontal', label: '横图' },
    { value: 'vertical', label: '竖图' },
    { value: 'square', label: '方图' }
]

// const sortOptions = [
//     { value: 'id', label: 'id' },
//     { value: 'userId', label: '用户id' },
//     { value: 'createTime', label: '创建时间' },
//     { value: 'editTime', label: '编辑时间' }
// ]


/**
 * 重置搜索条件
 */
function doClear() {
    Object.keys(searchParams).forEach((key) => {
        searchParams[key] = undefined
    })
    dateRange.value = []
    props.onSearch?.(searchParams)
}

/**
 * 获取预设的tag, category列表
 */
async function getTagCategoryOptions() {
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
.picture-search-form {
    margin-bottom: 16px;
}

/* 核心布局：允许换行，并垂直居中 */
.search-form-container {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
}

.picture-search-form :deep(.ant-form-item) {
    margin-top: 16px;
    margin-right: 24px;
    /* 增加项之间的间距 */
}

/* 关键：将按钮组推向最右侧 */
.buttons-item {
    margin-left: auto;
    margin-right: 0 !important;
    /* 覆盖全局 margin-right */
}
</style>