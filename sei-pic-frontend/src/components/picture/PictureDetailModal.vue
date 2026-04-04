<script setup lang="ts">
import { deletePictureByIdUsingPost, getPictureVoByIdUsingPost } from '@/api/pictureController';
import { SPACE_PERMISSION_ENUM } from '@/constants/space';
import { downloadImage, formatSize, toHexColor } from '@/utils';
import { DeleteOutlined, DownloadOutlined, EditOutlined, ExclamationCircleFilled, UserOutlined } from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { computed, createVNode, h, ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';

// #region 1. 弹窗 Props 和 Emit 定义 (完全保留)
interface Props {
    id?: string | number;
    visible: boolean;
}
const props = defineProps<Props>();
const emit = defineEmits(['update:visible', 'done']);
// #endregion

const picture = ref<API.PictureVO>({});
const loading = ref(false);

// #region 2. 弹窗控制逻辑 (保留，用于点击外部关闭)
const handleCancel = () => {
    emit('update:visible', false);
};

const loadPictureData = async () => {
    if (!props.id) return;
    picture.value = {};
    loading.value = true;
    try {
        const res = await getPictureVoByIdUsingPost({ id: props.id });
        if (res.data.code === 0 && res.data.data) {
            picture.value = res.data.data;
        } else {
            message.error("获取图片失败," + res.data.message);
        }
    } catch (e: any) {
        message.error("获取图片失败");
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    loadPictureData();
});

watch(
    () => [props.id, props.visible],
    ([newId, isVisible], [oldId, oldVisible]) => {
        if (isVisible) {
            // 当弹窗打开，且 ID 与上次不同时，立即清空旧数据
            if (newId !== oldId) {
                picture.value = {};
            }
            loadPictureData();
        } else {
            // 2. 当弹窗关闭时，也可以选择主动清空，确保下次打开是干净的
            picture.value = {};
        }
    }
);
// #endregion

// 3. 核心业务逻辑 (完全从 P1 复用)
const doCopy = (color: string) => {
    navigator.clipboard.writeText(color);
    message.success("成功复制主色调");
};

const router = useRouter();

const doEdit = () => {
    if (canEdit.value) {
        router.push(`/picture/add?id=${picture.value.id}&spaceId=${picture.value.spaceId}`);
        handleCancel(); // 跳转前关闭弹窗
    }
};

const doDelete = async () => {
    Modal.confirm({
        title: '确认删除该图片吗？',
        icon: createVNode(ExclamationCircleFilled),
        content: '删除后无法恢复，请谨慎操作。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        async onOk() {
            const res = await deletePictureByIdUsingPost({
                id: picture.value.id
            });
            try {
                if (res.data.code === 0 && res.data.data) {
                    message.success("删除成功");
                    emit('done');
                    handleCancel();
                } else {
                    message.error("删除失败," + res.data.message);
                }
            } catch (e: any) {
                message.error("删除失败," + e.message);
            }
        },
        onCancel() { }
    });
};

const doDownload = () => {
    let compressedUrl = picture.value.url;
    let lastDotIndex = compressedUrl?.lastIndexOf(".");
    let url = compressedUrl?.slice(0, lastDotIndex + 1) + picture.value.picFormat;
    downloadImage(url);
};

// #region 4. 权限校验逻辑 (完全从 P1 复用)
function createPermissionChecker(permission: string) {
    return computed(() => {
        return (picture.value.permissionList ?? []).includes(permission);
    });
}
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT);
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE);
// #endregion
</script>

<template>
    <a-modal :open="visible" :footer="null" :closable="false" :width="1300" @cancel="handleCancel" destroyOnClose
        centered wrapClassName="transparent-modal-wrap" maskClassName="blurry-mask"
        :bodyStyle="{ padding: 0, background: 'transparent' }">
        <a-spin :spinning="loading">
            <div id="pictureDetailModalInner" class="modal-content-container">
                <!-- 图片预览 -->
                <a-row :gutter="[24, 24]">
                    <a-col :sm="24" :md="16" :xl="18">
                        <a-card title="图片预览" class="main-card">
                            <div class="image-viewer">
                                <a-image :src="picture.url" class="display-image" />
                            </div>
                        </a-card>
                    </a-col>
                    <!-- 图片信息 -->
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
                                    <a-col :span="24">
                                        <a-space>
                                            <div class="info-label">主色调: </div>
                                            <div v-if="picture.picColor" :style="{
                                                backgroundColor: toHexColor(picture.picColor),
                                                width: '16px',
                                                height: '16px',
                                            }" />
                                            <span class="copyable-text" @click="doCopy(picture?.picColor?.slice(2))"
                                                title="点击复制主色调">
                                                {{ picture.picColor ? `${picture.picColor.slice(2)}` : '-' }}
                                            </span>
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
                                        v-if="canDelete">
                                        删除
                                    </a-button>
                                    <a-button size="small" type="primary" @click="doDownload"
                                        :icon="h(DownloadOutlined)">
                                        下载
                                    </a-button>
                                </a-space>
                            </div>
                        </a-card>
                    </a-col>
                </a-row>
            </div>
        </a-spin>
    </a-modal>
</template>

<style scoped>
/* #region 1. 深度定制 Modal，使其不可见且拥有磨砂遮罩 */

/* 定制遮罩层：复现原灰色背景 + 增加磨砂模糊效果 */
:deep(.blurry-mask) {
    background-color: rgba(240, 242, 245, 0.5) !important;
    /* 复现原灰色 page 背景色，但设置半透明 */
    backdrop-filter: blur(8px) !important;
    /* 关键：追加磨砂玻璃效果 */
    /* 适配某些老旧浏览器 */
    -webkit-backdrop-filter: blur(8px);
}

/* 定制弹窗包裹层：完全透明，去掉显式窗口感 */
:deep(.transparent-modal-wrap) {
    background: transparent !important;
}

/* 定制弹窗本身：透明背景，去掉阴影（因为 Card 自带投影） */
:deep(.transparent-modal-wrap .ant-modal-content) {
    background: transparent !important;
    box-shadow: none !important;
    border: none !important;
    padding: 0 !important;
}

/* 由于隐藏了 closable，确保 Modal 内部不会预留 header 空间 */
:deep(.ant-modal-header) {
    display: none !important;
}

/* #endregion */

/* #region 2. 完美复现原 picturedetailpage.vue 的所有样式 */

/* 容器基础样式 */
.modal-content-container {
    padding: 0;
    /* 移除外层 padding，让 Card 直接靠 Modal 边缘或外部 */
    background: transparent;
    /* 弹窗背景必须透明 */
}

/* --- 左侧图片展示区域样式 --- (完全复现 P1) */
.main-card {
    border-radius: 12px;
    height: 100%;
    min-height: 65vh;
    overflow: hidden;
    /* 关键：保留原 P1 的 Card 投影 */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    background: #fff;
    /* Card 自身是白色的 */
}

.main-card :deep(.ant-card-body) {
    padding: 0;
    /* 关键：复现原 P1 的半透明灰色衬托背景 */
    background-color: #89868658;
    height: 65vh;
    display: flex;
    justify-content: center;
    align-items: center;
}

.image-viewer {
    width: 100%;
    height: 100%;
    padding: 32px;
    display: flex;
    justify-content: center;
    align-items: center;
}

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
    /* 关键：复现原 P1 的图片边缘浮雕投影效果 */
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

/* --- 右侧信息展示栏样式 --- (完全复现 P1) */
.info-card {
    height: 100%;
    min-height: 65vh;
    display: flex;
    flex-direction: column;
    border-radius: 12px;
    /* 关键：保留原 P1 的 Card 投影 */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    background: #fff;
    /* Card 自身是白色的 */
}

.info-card :deep(.ant-card-body) {
    flex: 1;
    display: flex;
    flex-direction: column;
}

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
}

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

.action-footer {
    margin-top: auto;
    padding-top: 24px;
    display: flex;
    justify-content: center;
}

.tags-group {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 4px;
}

.tags-group :deep(.ant-tag) {
    margin: 0;
    border-radius: 4px;
    font-size: 12px;
}

.copyable-text {
    cursor: pointer;
    transition: opacity 0.2s;
}

.copyable-text:hover {
    opacity: 0.8;
}

@media screen and (max-width: 768px) {
    .main-card :deep(.ant-card-body) {
        height: 40vh;
    }
}

/* #endregion */
</style>