<template>
    <div class="pictureUpload">
        <a-upload list-type="picture-card" :show-upload-list="false" :before-upload="beforeUpload"
            :custom-request="handleUpload">
            <img v-if="imageUrl || picture?.url" :src="imageUrl || picture?.url" alt="picture" />
            <div v-else>
                <loading-outlined v-if="loading"></loading-outlined>
                <plus-outlined v-else></plus-outlined>
                <div class="ant-upload-text">点击或拖拽上传图片</div>
            </div>
        </a-upload>
    </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import type { UploadProps } from 'ant-design-vue';
import { uploadPictureUsingPost } from '@/api/pictureController';

const loading = ref<boolean>(false);

interface Props {
    picture?: API.PictureVO,
    onSuccess?: (newPicture: API.PictureVO) => void;
}

const imageUrl = ref<string>('');

const props = defineProps<Props>()




/**
 * 上传前校验
 * @param file 图片
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
    const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
    if (!isJpgOrPng) {
        message.error('You can only upload JPG file!');
    }
    const isLt2M = file.size / 1024 / 1024 < 2;
    if (!isLt2M) {
        message.error('Image must smaller than 2MB!');
    }
    return isJpgOrPng && isLt2M;
};

const handleUpload = async ({ file }: any) => {
    loading.value = true

    try {
        const params = props.picture ? { id: props.picture.id } : {}
        const res = await uploadPictureUsingPost(params, {}, file)
        if (res.data.code === 0 && res.data.data) {
            message.success("图片上传成功")
            const newPicture: API.PictureVO = res.data?.data
            imageUrl.value = newPicture.url

            if (props.onSuccess) {
                props.onSuccess(newPicture);
            }
        } else {
            message.error("图片上传失败," + res.data.message)
        }
    } catch (e: any) {
        message.error("图片上传失败," + e.message);
    }
}

</script>
<style scoped>
.pictureUpload :deep(.ant-upload) {
    width: 100% !important;
    height: 100% !important;
    min-width: 152px;
    min-height: 152px;
}

.pictureUpload img {
    max-width: 100%;
    max-width: 480px;
}
</style>
