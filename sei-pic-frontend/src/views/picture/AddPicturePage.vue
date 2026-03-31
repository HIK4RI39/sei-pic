<template>
    <div id="addPicturePage">
        <h2 style="margin-bottom: 16px; align-content: center;">{{ route.query?.id ? '修改图片' : '创建图片' }}</h2>

        <!-- 选择上传方式 -->
        <a-tabs v-model:activeKey="uploadType">>
            <a-tab-pane key="file" tab="文件上传">
                <PictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
            </a-tab-pane>
            <a-tab-pane key="url" tab="URL 上传" force-render>
                <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
            </a-tab-pane>
        </a-tabs>

        <!-- 图片信息 -->
        <a-form v-if="picture" name="pictureForm" layout="horizontal" :model="pictureForm" @finish="handleSubmit">
            <!-- name -->
            <a-form-item label="名称: " name="name">
                <a-input v-model:value="pictureForm.name" placeholder="图片名称" />
            </a-form-item>
            <!-- introduction -->
            <a-form-item label="简介: " name="introduction">
                <a-textarea v-model:value="pictureForm.introduction" placeholder="图片简介"
                    :autoSize="{ minRows: 2, maxRows: 5 }" />
            </a-form-item>
            <!-- category -->
            <a-form-item label="分类: " name="category">
                <a-select v-model:value="pictureForm.category" placeholder="请选择分类" :options="categoryOptions"
                    allow-clear />
            </a-form-item>
            <!-- tags -->
            <a-form-item label="标签: " name="tags">
                <a-form-item>
                    <a-select v-model:value="pictureForm.tags" mode="tags" placeholder="请选择标签" :options="tagOptions"
                        allow-clear />
                </a-form-item>
            </a-form-item>
            <!-- submit -->
            <a-form-item>
                <a-button type="primary" style="width: 100%;" html-type="submit">{{ route.query?.id ? '修改' : '创建'
                }}</a-button>
            </a-form-item>
        </a-form>
    </div>
</template>

<script setup lang="ts">
import { editPictureUsingPost, getPictureVoByIdUsingPost, listPictureTagCategoryUsingGet } from '@/api/pictureController';
import PictureUpload from '@/components/PictureUpload.vue';
import UrlPictureUpload from '@/components/UrlPictureUpload.vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive } from 'vue';
import { computed } from 'vue';
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter()
const route = useRoute()

const picture = ref<API.PictureVO>()
const uploadType = ref<'file' | 'url'>('file')

// 空间 id
const spaceId = computed(() => {
    return route.query?.spaceId
})


const onSuccess = (newPicture: API.PictureVO) => {
    picture.value = newPicture
    pictureForm.name = newPicture.name

    // console.log("收到回调: ", newPicture)
}

const pictureForm = reactive<API.PictureEditRequest>({})

const handleSubmit = async (values: any) => {
    const pictureId = picture.value?.id
    if (!pictureId) {
        return
    }

    const res = await editPictureUsingPost({
        id: pictureId,
        spaceId: spaceId.value,
        ...values
    })
    if (res.data.code === 0 && res.data.data) {
        message.success("创建图片成功")
        // 创建成功, 跳转图片详情页
        router.push({
            path: `/picture/${pictureId}`
        })
    } else {
        message.error("创建图片失败," + res.data.message)
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

const getOldPicture = async () => {
    const id = route.query?.id
    if (id) {
        const res = await getPictureVoByIdUsingPost({ id })
        if (res.data.code === 0 && res.data.data) {
            const data = res.data.data
            // 图片
            picture.value = data
            // 表单
            pictureForm.name = data.name
            pictureForm.introduction = data.introduction
            pictureForm.category = data.category
            pictureForm.tags = data.tags
        } else {
            message.error("获取图片失败," + res.data.message)
        }
    }
}

onMounted(() => {
    getOldPicture();
})



</script>

<style scoped>
#addPicturePage {
    max-width: 720px;
    margin: 0 auto;
}
</style>