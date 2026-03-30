<template>
    <div id="batchAddPicturePage">
        <a-card title="智能批量抓取图片" :bordered="false" class="container">
            <a-form :model="params" layout="vertical" @finish="handleUpload">
                <a-form-item label="搜索关键词" name="searchText" extra="根据关键词从外部引擎抓取相关图片">
                    <a-input v-model:value="params.searchText" placeholder="例如：程序员、宇宙、可爱猫咪" size="large" allow-clear />
                </a-form-item>

                <a-form-item label="图片名称前缀" name="namePrefix" extra="不填则默认使用搜索关键词作为前缀">
                    <a-input v-model:value="params.namePrefix" placeholder="例如：coder_style" size="large" allow-clear />
                </a-form-item>

                <a-form-item label="抓取数量" name="count">
                    <a-select v-model:value="params.count" placeholder="请选择拉取的图片数量" size="large" :options="[
                        { label: '10 张图片', value: 10 },
                        { label: '20 张图片', value: 20 },
                        { label: '30 张图片', value: 30 }
                    ]" />
                </a-form-item>

                <a-form-item label="分类" name="category">
                    <a-select v-model:value="params.category" placeholder="输入或选择分类(可以为空)" size="large"
                        :options="categoryOptions" show-search allow-clear />
                </a-form-item>

                <a-form-item label="标签" name="tags">
                    <a-select v-model:value="params.tags" mode="tags" placeholder="输入或选择标签(可以为空)" size="large"
                        :options="tagOptions" allow-clear />
                </a-form-item>


                <a-form-item style="margin-top: 24px">
                    <a-button type="primary" html-type="submit" size="large" block :loading="loading">
                        开始执行批量拉取
                    </a-button>
                </a-form-item>
            </a-form>

            <div class="tip-box">
                <p>💡 提示：抓取过程可能耗时较长，请耐心等待任务完成提示。</p>
            </div>
        </a-card>
    </div>
</template>
<script setup lang="ts">
import { listPictureTagCategoryUsingGet, uploadPictureByBatchUsingPost, uploadPictureByUrlUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { onMounted, ref } from 'vue'

const loading = ref<boolean>(false)

const params = ref<API.PictureUploadByBatchRequest>({
    searchText: '',
    count: 10
})

/**  
 * 上传  
 */
const handleUpload = async () => {
    if (!params.value.searchText) {
        message.error("搜索关键词不能为空")
        return
    }

    loading.value = true
    try {
        const res = await uploadPictureByBatchUsingPost({
            ...params.value
        }, {
            timeout: 60000
        })
        if (res.data.code === 0 && res.data.data) {
            message.success(`成功抓取${res.data.data}张图片`)
        } else {
            message.error('图片拉取失败，' + res.data.message)
        }
    } catch (error) {
        message.error('图片拉取失败')
    } finally {
        loading.value = false
    }
}

const tagOptions = ref<string[] | undefined>([])
const categoryOptions = ref<string[] | undefined>([])

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
#batchAddPicturePage {
    padding: 40px 20px;
    background: #f5f5f5;
    min-height: 80vh;
    display: flex;
    justify-content: center;
}

.container {
    width: 100%;
    max-width: 600px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.tip-box {
    margin-top: 16px;
    padding: 12px;
    background-color: #e6f7ff;
    border: 1px solid #91d5ff;
    border-radius: 4px;
    color: rgba(0, 0, 0, 0.65);
    font-size: 13px;
}
</style>