<template>
    <div class="vip-exchange-wrapper">
        <div class="vip-card-container">
            <div class="vip-card-header">
                <div class="gold-badge">VIP</div>
                <h2 class="vip-title">尊享会员兑换</h2>
                <p class="vip-desc">请输入您的专属兑换码，即刻开启尊贵体验</p>
            </div>

            <div class="vip-form">
                <div class="input-group">
                    <label class="input-label">兑换码</label>
                    <a-input v-model:value="vipCode.code" placeholder="请输入兑换码" size="large" class="custom-gold-input"
                        allow-clear />
                </div>

                <a-button type="primary" block size="large" class="gold-submit-btn" :loading="loading"
                    @click="handleExchange">
                    立即兑换
                </a-button>
            </div>

            <div class="vip-card-footer">
                <span>* 请核对兑换码大小写，兑换成功后不可撤回</span>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useLoginUserStore } from '@/stores/useLoginStore';
import { useRouter } from 'vue-router';
import { exchangeVipUsingPost, getLoginUserUsingGet, getLoginUserWithoutCacheUsingGet } from '@/api/userController';
import { USER_ROLE_ENUM } from '@/constants/user';

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 如果未登录 => 登录
onMounted(() => {
    if (!loginUserStore.loginUser?.id) {
        router.push('/user/login')
    }
})


// 定义接口类型
interface VipCode {
    code?: string;
}

// 响应式数据
const vipCode = ref<VipCode>({
    code: '',
});
const loading = ref<boolean>(false);

/**
 * 兑换逻辑处理
 * exchangeVipUsingPost({code: vipCode.value.code})
 */
const handleExchange = async () => {
    if (!vipCode.value.code || vipCode.value.code.trim() === '') {
        message.warning('请输入有效的兑换码');
        return;
    }

    loading.value = true;
    try {
        // 调用您的已有接口
        const res = await exchangeVipUsingPost({ code: vipCode.value.code });
        // A7b9C2dE
        if (res.data.code === 0 && res.data.data) {
            message.success('兑换成功！您的VIP特权已生效');
            vipCode.value.code = ''; // 兑换后清空

            // 2. 关键：重新请求后端获取最新的用户信息（含新的 role 和 vipNumber）
            const newUserRes = await getLoginUserWithoutCacheUsingGet();
            if (newUserRes.data.code === 0) {
                // 更新全局状态，这样刷新后，只要 App.vue 逻辑正确，就能保持状态
                loginUserStore.setLoginUser(newUserRes.data.data);
            }

        } else {
            message.error('兑换失败，请稍后重试或联系客服, ' + res.data.message);
        }
    } catch (error) {
        message.error('兑换失败，请稍后重试或联系客服');
    } finally {
        loading.value = false;
    }
};
</script>

<style scoped>
/* 容器：垂直居中，背景采用柔和的浅灰 */
.vip-exchange-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background-color: #fcfcfc;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 卡片主体：简约轻奢风 */
.vip-card-container {
    width: 100%;
    max-width: 400px;
    background: #ffffff;
    padding: 40px 32px;
    border-radius: 24px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(212, 175, 55, 0.1);
}

/* 顶部装饰 */
.vip-card-header {
    text-align: center;
    margin-bottom: 32px;
}

.gold-badge {
    display: inline-block;
    padding: 4px 12px;
    background: linear-gradient(135deg, #d4af37 0%, #f9e498 100%);
    color: #ffffff;
    border-radius: 50px;
    font-size: 12px;
    font-weight: bold;
    letter-spacing: 1px;
    margin-bottom: 16px;
}

.vip-title {
    font-size: 22px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0 0 8px 0;
}

.vip-desc {
    font-size: 14px;
    color: #8c8c8c;
}

/* 输入框定制 */
.input-group {
    margin-bottom: 24px;
}

.input-label {
    display: block;
    font-size: 13px;
    color: #595959;
    margin-bottom: 8px;
    font-weight: 500;
}

.custom-gold-input {
    border-radius: 10px !important;
    height: 48px !important;
    border: 1px solid #e8e8e8 !important;
}

.custom-gold-input:focus,
.custom-gold-input:hover {
    border-color: #d4af37 !important;
    box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.1) !important;
}

/* 金色按钮：核心视觉点 */
.gold-submit-btn {
    height: 50px !important;
    border-radius: 12px !important;
    border: none !important;
    font-weight: 600 !important;
    font-size: 16px !important;
    /* 经典的拉丝金/香槟金渐变 */
    background: linear-gradient(135deg, #d4af37 0%, #edc967 50%, #d4af37 100%) !important;
    color: #ffffff !important;
    box-shadow: 0 6px 16px rgba(212, 175, 55, 0.25) !important;
    transition: all 0.3s ease !important;
}

.gold-submit-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(212, 175, 55, 0.35) !important;
    filter: brightness(1.05);
}

.gold-submit-btn:active {
    transform: translateY(0);
}

/* 底部备注 */
.vip-card-footer {
    text-align: center;
    margin-top: 24px;
}

.vip-card-footer span {
    font-size: 12px;
    color: #bfbfbf;
}
</style>