<template>
    <div class="picture-search-form">
        <!-- 搜索表单 -->
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="关键词" name="searchText">
                <a-input v-model:value="searchParams.searchText" placeholder="从名称和简介搜索" allow-clear />
            </a-form-item>
            <!-- category -->
            <a-form-item label="分类" name="category">
                <a-auto-complete v-model:value="searchParams.category" style="min-width: 180px"
                    :options="categoryOptions" placeholder="请输入分类" allowClear />
            </a-form-item>
            <!-- tags -->
            <a-form-item label="标签" name="tags">
                <a-select v-model:value="searchParams.tags" style="min-width: 180px" :options="tagOptions" mode="tags"
                    placeholder="请输入标签" allowClear />
            </a-form-item>
            <a-form-item label="日期" name="">
                <a-range-picker style="width: 400px" show-time v-model:value="dateRange"
                    :placeholder="['编辑开始日期', '编辑结束时间']" format="YYYY/MM/DD HH:mm:ss" :presets="rangePresets"
                    @change="onRangeChange" />
            </a-form-item>
            <a-form-item label="名称" name="name">
                <a-input v-model:value="searchParams.name" placeholder="请输入名称" allow-clear />
            </a-form-item>
            <a-form-item label="简介" name="introduction">
                <a-input v-model:value="searchParams.introduction" placeholder="请输入简介" allow-clear />
            </a-form-item>
            <a-form-item label="宽度" name="picWidth">
                <a-input-number v-model:value="searchParams.picWidth" />
            </a-form-item>
            <a-form-item label="高度" name="picHeight">
                <a-input-number v-model:value="searchParams.picHeight" />
            </a-form-item>
            <!-- format -->
            <a-form-item label="格式" name="picFormat">
                <a-auto-complete v-model:value="searchParams.category" style="min-width: 180px" :options="formatOptions"
                    placeholder="请选择格式" allowClear />
            </a-form-item>
            <!-- 搜索 -->
            <!-- 重置 -->
            <a-form-item>
                <a-space>
                    <a-button type="primary" html-type="submit" style="width: 96px">搜索</a-button>
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
/* 表单项边距 */
.picture-search-form {
    margin-bottom: 16px;
}

.picture-search-form .ant-form-item {
    margin-top: 16px;
}
</style>