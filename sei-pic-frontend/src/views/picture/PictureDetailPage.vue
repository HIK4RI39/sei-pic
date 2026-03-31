<script setup lang="ts">
import { deletePictureByIdUsingPost, editPictureUsingPost, getPictureVoByIdUsingPost } from '@/api/pictureController';
import { useLoginUserStore } from '@/stores/useLoginStore';
import { downloadImage, formatSize } from '@/utils';
import { DeleteOutlined, DownloadOutlined, EditOutlined, ExclamationCircleFilled } from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { computed, createVNode, h } from 'vue';
import { ref } from 'vue';
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

interface Props {
    id: string | number
}
const props = defineProps<Props>()

const picture = ref<API.PictureVO>({})

const fetchDetailPicture = async () => {
    const res = await getPictureVoByIdUsingPost({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
        const data = res.data.data
        // 图片
        picture.value = data
    } else {
        message.error("获取图片失败," + res.data.message)
    }
}

onMounted(() => {
    fetchDetailPicture();
})

const loginUserStore = useLoginUserStore()
const router = useRouter()

const canEdit = computed(() => {
    const loginUser = loginUserStore.loginUser
    if (!loginUser.id) {
        return false
    }
    const user = picture.value.user || {}
    return loginUser.id === user.id || loginUser.userRole === 'admin'
})

const doEdit = () => {
    if (canEdit) {
        router.push('/picture/add?id=' + picture.value.id)
    }
}

const doDelete = async () => {
    Modal.confirm({
        title: '确认删除该图片吗？',
        icon: createVNode(ExclamationCircleFilled),
        content: '删除后无法恢复，请谨慎操作。',
        okText: '确认删除',
        okType: 'danger', // 按钮颜色变为红色，提醒这是危险操作
        cancelText: '取消',
        // 点击确认后的回调
        async onOk() {
            const res = await deletePictureByIdUsingPost({
                id: picture.value.id
            })
            try {
                if (res.data.code === 0 && res.data.data) {
                    message.success("删除成功")
                    // 跳转回前一个页面
                    if (window.history.state && window.history.state.back) {
                        router.back();
                    }
                } else {
                    message.error("删除失败," + res.data.message)
                }
            } catch (e: any) {
                message.error("删除失败," + e.message);
            }
        }, onCancel() { }
    })
}

const doDownload = () => {
    // 下载原图, 从format拿到后缀, 自行拼接url
    let compressedUrl = picture.value.url
    let lastDotIndex = compressedUrl?.lastIndexOf(".")
    let url = compressedUrl?.slice(0, lastDotIndex + 1) + picture.value.picFormat
    downloadImage(url)
}

</script>

<template>
    <div id="pictureDetailPage" class="page-container">
        <a-row :gutter="[24, 24]">
            <a-col :sm="24" :md="16" :xl="18">
                <a-card title="图片预览" class="main-card">
                    <div class="image-viewer">
                        <a-image :src="picture.url" class="display-image" />
                    </div>
                </a-card>
            </a-col>

            <a-col :sm="24" :md="8" :xl="6">
                <a-card title="图片信息" class="info-card">
                    <div class="user-info-row">
                        <a-avatar :size="48" :src="picture.user?.userAvatar">
                            <template #icon>
                                <UserOutlined />
                            </template>
                        </a-avatar>
                        <div class="user-text">
                            <div class="username">{{ picture.user?.userName || '未知用户' }}</div>
                            <div class="user-id">ID: {{ picture.user?.id || '匿名' }}</div>
                        </div>
                    </div>

                    <a-divider style="margin: 16px 0" />

                    <div class="info-details">
                        <a-row :gutter="[0, 16]">
                            <a-col :span="24">
                                <div class="info-label">名称</div>
                                <div class="info-value">{{ picture.name ?? '未命名' }}</div>
                            </a-col>
                            <a-col :span="24">
                                <div class="info-label">简介</div>
                                <div class="info-value intro">{{ picture.introduction ?? '暂无简介' }}</div>
                            </a-col>
                            <a-col :span="24">
                                <div class="info-label">参数</div>
                                <div class="info-value">
                                    <a-space separator="|">
                                        <span>{{ picture.picFormat?.toUpperCase() }}</span>
                                        <span>{{ formatSize(picture.picSize) }}</span>
                                        <span>{{ picture.picWidth }} x {{ picture.picHeight }}</span>
                                    </a-space>
                                </div>
                            </a-col>
                            <a-col :span="24">
                                <a-space>
                                    <div class="info-label">分类: </div>
                                    <a-tag color="blue">{{ picture.category || '默认' }}</a-tag>
                                </a-space>
                            </a-col>
                            <a-col :span="24">
                                <a-space>
                                    <div class="info-label">标签: </div>
                                    <div class="tags-group">
                                        <template v-if="picture.tags && picture.tags.length > 0">
                                            <a-tag v-for="tag in picture.tags" :key="tag" color="green">
                                                {{ tag }}
                                            </a-tag>
                                        </template>
                                        <span v-else class="info-value">-</span>
                                    </div>
                                </a-space>

                            </a-col>
                        </a-row>
                    </div>

                    <div class="action-footer">
                        <a-space size="small">
                            <a-button size="small" type="primary" ghost @click="doEdit" :icon="h(EditOutlined)"
                                v-if="canEdit">
                                编辑
                            </a-button>
                            <a-button size="small" danger ghost @click="doDelete" :icon="h(DeleteOutlined)"
                                v-if="canEdit">
                                删除
                            </a-button>
                            <a-button size="small" type="primary" @click="doDownload" :icon="h(DownloadOutlined)">
                                下载
                            </a-button>
                        </a-space>
                    </div>
                </a-card>
            </a-col>
        </a-row>
    </div>
</template>

<style scoped>
/* 页面背景与基础容器 */
.page-container {
    padding: 24px;
    background: #f0f2f5;
    min-height: 100vh;
}

/* --- 左侧图片展示区域样式 --- */
.main-card {
    border-radius: 12px;
    height: 100%;
    min-height: 65vh;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.main-card :deep(.ant-card-body) {
    padding: 0;
    background-color: #89868658;
    /* 黑色背景衬托 */
    /* background-color: transparent; */
    /* 改为透明 */
    height: 65vh;
    /* 设为视口高度的 65%，确保不超屏 */
    display: flex;
    justify-content: center;
    align-items: center;
}

.image-viewer {
    width: 100%;
    height: 100%;
    padding: 32px;
    /* 产生呼吸感缩放效果 */
    display: flex;
    justify-content: center;
    align-items: center;
}

/* 强制 AntD Image 组件适配容器 */
.image-viewer :deep(.ant-image) {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}

.image-viewer :deep(.ant-image-img) {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
    /* 保证比例且完整显示 */
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
    /* 给图片增加投影 */
}

/* --- 右侧信息展示栏样式 --- */
.info-card {
    height: 100%;
    min-height: 65vh;
    /* 保证与左侧视觉平衡 */
    display: flex;
    flex-direction: column;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.info-card :deep(.ant-card-body) {
    flex: 1;
    display: flex;
    flex-direction: column;
}

/* 用户信息模块：头像 + 昵称/ID */
.user-info-row {
    display: flex;
    align-items: center;
    gap: 12px;
}

.user-text {
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.username {
    font-size: 16px;
    font-weight: 600;
    color: #262626;
    line-height: 1.4;
}

.user-id {
    font-size: 12px;
    color: #bfbfbf;
    margin-top: -2px;
    /* 稍微向上靠拢 */
}

/* 信息项列表排版 */
.info-details {
    flex: 1;
}

.info-label {
    font-size: 13px;
    color: #8c8c8c;
    margin-bottom: 4px;
}

.info-value {
    font-size: 14px;
    color: #262626;
    font-weight: 500;
}

.info-value.intro {
    background: #fafafa;
    padding: 10px;
    border-radius: 6px;
    border: 1px solid #f0f0f0;
    color: #595959;
    font-weight: normal;
}

/* 底部操作区：水平居中并固定在卡片底部 */
.action-footer {
    margin-top: auto;
    /* 关键：推到底部 */
    padding-top: 24px;
    display: flex;
    justify-content: center;
    /* 按钮居中 */
}

.action-footer .ant-btn {
    border-radius: 8px;
    /* padding: 0 20px; */
    /* height: 36px; */
}

/* 响应式：针对窄屏幕（如平板）调整 */
@media screen and (max-width: 768px) {
    .main-card :deep(.ant-card-body) {
        height: 40vh;
    }
}

.tags-group {
    display: flex;
    flex-wrap: wrap;
    /* 关键：标签多了自动换行 */
    gap: 6px;
    /* 标签之间的间距 */
    margin-top: 4px;
}

.tags-group :deep(.ant-tag) {
    margin: 0;
    /* 清除 AntD 默认的外边距，由 gap 统一控制 */
    border-radius: 4px;
    font-size: 12px;
}

.info-label {
    font-size: 13px;
    color: #8c8c8c;
    margin-bottom: 4px;
}

.info-value {
    font-size: 14px;
    color: #262626;
    font-weight: 500;
}
</style>