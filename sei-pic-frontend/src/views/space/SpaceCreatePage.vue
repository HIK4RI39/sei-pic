<template>
    <div id="addSpacePage">
        <a-card title="创建空间" class="add-card">
            <a-form :model="form" layout="vertical" @finish="handleSubmit" ref="formRef" :rules="rules">
                <a-form-item label="空间名称" name="spaceName">
                    <a-input v-model:value="form.spaceName" placeholder="请输入空间名称" allow-clear />
                </a-form-item>

                <a-form-item label="空间级别" name="spaceLevel">
                    <a-select v-model:value="form.spaceLevel" placeholder="请选择空间级别" :options="SPACE_LEVEL_OPTIONS" />
                </a-form-item>

                <a-form-item>
                    <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
                        提交
                    </a-button>
                </a-form-item>
            </a-form>
        </a-card>
        <a-card title="空间级别介绍" class="info-card" :bordered="false">
            <div class="notice-wrapper">
                <a-alert type="info" show-icon>
                    <template #message>
                        目前仅支持开通普通版，如需升级空间，请联系
                        <a href="https://github.com/HIK4RI39" target="_blank" class="link-user">网站管理员</a>
                    </template>
                </a-alert>
            </div>
            <div class="info-content">
                <div class="level-row">
                    <div class="level-label">🖼️普通版</div>
                    <a-descriptions :column="2" size="small" class="level-desc-inline">
                        <a-descriptions-item label="最大容量">
                            <a-tag color="green">100.00 MB</a-tag>
                        </a-descriptions-item>
                        <a-descriptions-item label="图片数量">
                            <a-tag color="green">100 张</a-tag>
                        </a-descriptions-item>
                    </a-descriptions>
                </div>
                <div class="level-row">
                    <div class="level-label">🚀专业版</div>
                    <a-descriptions :column="2" size="small" class="level-desc-inline">
                        <a-descriptions-item label="最大容量">
                            <a-tag color="blue">1000.00 MB</a-tag>
                        </a-descriptions-item>
                        <a-descriptions-item label="图片数量">
                            <a-tag color="blue">1000 张</a-tag>
                        </a-descriptions-item>
                    </a-descriptions>
                </div>
                <div class="level-row">
                    <div class="level-label">💎旗舰版</div>
                    <a-descriptions :column="2" size="small" class="level-desc-inline">
                        <a-descriptions-item label="最大容量">
                            <a-tag color="purple">10000.00 MB</a-tag>
                        </a-descriptions-item>
                        <a-descriptions-item label="图片数量">
                            <a-tag color="purple">10000 张</a-tag>
                        </a-descriptions-item>
                    </a-descriptions>
                </div>
            </div>
        </a-card>
    </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { createSpaceUsingPost } from '@/api/spaceController';
import { SPACE_LEVEL_OPTIONS } from '@/constants/space'; // 引入常量文件
import { DatabaseFilled, DatabaseOutlined, RocketFilled, RocketOutlined } from '@ant-design/icons-vue';

const route = useRoute();
const router = useRouter();
const formRef = ref();
const loading = ref(false);

// 表单数据初始状态
const form = ref<API.SpaceAddRequest | API.SpaceUpdateRequest>({
    spaceName: '',
    spaceLevel: 0, // 默认为普通版
});

// 计算属性：判断当前是创建还是修改 (根据 URL 是否有 id 参数)
const spaceId = computed(() => route.query.id as string);
const isEditPage = computed(() => !!spaceId.value);

// 表单校验规则
const rules = {
    spaceName: [{ required: true, message: '空间名称不能为空', trigger: 'blur' }],
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
    loading.value = true;
    try {
        const res = await createSpaceUsingPost({ ...form.value });
        if (res.data.code === 0) {
            message.success('创建成功');
            // TODO 跳转个人空间页面
            router.push('/space_manage');
        } else {
            message.error('创建失败: ' + res.data.message);
        }
    } finally {
        loading.value = false;
    }
};

</script>

<style scoped>
#addSpacePage {
    max-width: 50%;
    margin: 0 auto;
}

.add-card {
    margin-bottom: 24px;
}

.info-card {
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
    margin-top: 24px;
}

.notice-wrapper {
    margin-bottom: 20px;
}

/* 级别行容器 */
.level-row {
    display: flex;
    align-items: center;
    padding: 16px 0;
    /* 增加上下间距，更透气 */
    border-bottom: 1px solid #f5f5f5;
}

.level-row:last-child {
    border-bottom: none;
}

/* 左侧标题 */
.level-label {
    font-weight: bold;
    font-size: 15px;
    color: #1a1a1a;
    width: 100px;
    /* 增加宽度 */
    flex-shrink: 0;
}

/* 描述列表容器 */
.level-desc-inline {
    flex: 1;
}

/* 核心修改：限制 item 的宽度，让内容靠左对齐，不再铺满全行 */
:deep(.ant-descriptions-item) {
    padding-bottom: 0 !important;
    width: 200px;
    /* 固定每一项的宽度，让间距统一 */
}

/* 统一 label 和 content 的对齐方式 */
:deep(.ant-descriptions-item-label) {
    color: #666;
    font-weight: normal;
}

:deep(.ant-descriptions-item-content) {
    display: flex;
    align-items: center;
}

/* 移除 Ant Design 默认标题栏边距 */
:deep(.ant-descriptions-header) {
    margin-bottom: 0 !important;
}

.link-user {
    font-weight: 500;
    color: #1890ff;
    text-decoration: underline;
}

.link-user:hover {
    color: #40a9ff;
}

.info-content {
    font-size: 14px;
    margin: 0 auto;
}
</style>