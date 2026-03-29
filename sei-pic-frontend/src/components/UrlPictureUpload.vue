<template>
    <div class="url-picture-upload">
        <img v-if="picture?.url" :src="picture?.url" alt="avatar" style="max-width: 50%;" />
        <div style="margin-bottom: 16px;"></div>
        <a-input-group compact style="margin-bottom: 16px">
            <a-input v-model:value="fileUrl" style="width: calc(100% - 120px)" placeholder="请输入图片 URL" />
            <a-button type="primary" :loading="loading" @click="handleUpload" style="width: 120px">提交</a-button>
        </a-input-group>
    </div>
</template>

<script setup lang="ts">
import { uploadPictureByUrlUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { ref } from 'vue'

const loading = ref<boolean>(false)
const fileUrl = ref<string>()

interface Props {
    picture?: API.PictureVO,
    onSuccess?: (newPicture: API.PictureVO) => void;
}
const props = defineProps<Props>()



/**  
 * 上传  
 */
const handleUpload = async () => {
    loading.value = true
    try {
        const params: API.PictureUploadRequest = { fileUrl: fileUrl.value }
        if (props.picture) {
            params.id = props.picture.id
        }
        const res = await uploadPictureByUrlUsingPost(params)
        if (res.data.code === 0 && res.data.data) {
            message.success('图片上传成功')
            // 将上传成功的图片信息传递给父组件  
            props.onSuccess?.(res.data.data)
        } else {
            message.error('图片上传失败，' + res.data.message)
        }
    } catch (error) {
        message.error('图片上传失败')
    } finally {
        loading.value = false
    }
}

</script>

<style scoped>
.url-picture-upload {
    display: flex;
    flex-direction: column;
    /* 让输入框和图片垂直排列 */
    align-items: center;
    /* 关键：让子元素在交叉轴（水平方向）居中 */
    width: 100%;
}

/* 确保输入框组依然占满宽度，不被 flex 压缩 */
.url-picture-upload :deep(.ant-input-group) {
    width: 100% !important;
    display: flex;
    /* 让输入框和按钮在一行 */
}

.url-picture-upload img {
    max-width: 100%;
    /* 防止图片超出容器 */
    margin-top: 16px;
    /* 与输入框保持间距 */
    border-radius: 8px;
    /* 可选：加点圆角更美观 */
    object-fit: contain;
    /* 保持比例 */
}
</style>