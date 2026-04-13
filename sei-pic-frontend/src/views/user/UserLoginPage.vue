<template>
    <div id="userLoginPage">
        <a-alert message="推荐使用 PC 浏览器访问，以获得最佳操作体验" type="info" show-icon banner />

        <div class="content-container">
            <div class="login-card">
                <div class="header">
                    <h1 class="title">𝑴𝒆𝒐𝒘𝑷𝒊𝒄 - 用户登录</h1>
                    <p class="desc">企业级智能协同云图库</p>
                </div>

                <a-form :model="formState" @finish="handleLogin" ref="formRef" layout="horizontal"
                    :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">

                    <div class="demo-actions">
                        演示账号:
                        <a @click="autoFill('user')">user</a>
                        <a-divider type="vertical" />
                        <a @click="autoFill('admin')">admin</a>
                    </div>
                    <a-form-item label="账号" name="userAccount" :rules="[
                        { required: true, message: '账号是必填项' },
                        { min: 4, max: 20, message: '账号长度必须在 4 到 20 个字符之间!', trigger: 'change' },
                    ]">
                        <div class="input-group">
                            <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large" />

                        </div>
                    </a-form-item>

                    <a-form-item label="密码" name="userPassword" :rules="[
                        { required: true, message: '密码是必填项' },
                        { min: 8, max: 20, message: '密码长度必须在 8 到 20 个字符之间!', trigger: 'change' },
                    ]">
                        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large" />
                    </a-form-item>

                    <a-form-item :wrapper-col="{ offset: 5, span: 19 }">
                        <div class="tips">
                            没有账号?
                            <RouterLink to="/user/register">立即注册</RouterLink>
                        </div>
                        <a-form-item>
                            <div style="margin-left: -15%; text-align: center; width: 120%;">
                                <a-button type="primary" style="width: 300px;" html-type="submit"
                                    size="large">登录</a-button>
                            </div>
                        </a-form-item>
                    </a-form-item>
                </a-form>
            </div>
        </div>

    </div>
</template>

<script lang="ts" setup>
import { loginUsingPost } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginStore'
import { message } from 'ant-design-vue'
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const formState = reactive<API.UserLoginRequest>({})
const loginUserStore = useLoginUserStore()
const router = useRouter()
const formRef = ref()

const autoFill = (role: 'user' | 'admin') => {
    formState.userAccount = role
    formState.userPassword = '12345678'
    if (formRef.value) {
        formRef.value.validateFields(['userAccount', 'userPassword'])
    }
}

const handleLogin = async () => {
    try {
        const res = await loginUsingPost({ ...formState })
        if (res.data.code === 0 && res.data.data) {
            await loginUserStore.fetchLoginUser()
            message.success('登录成功')
            router.push({ path: '/', replace: true })
        } else {
            message.error('登录失败：' + res.data.message)
        }
    } catch (e: any) {
        message.error('系统异常')
    }
}
</script>

<style scoped>
#userLoginPage {
    height: 100vh;
    background: #f0f2f5;
    /* 浅灰色背景突出白色卡片 */
    background-image: url('https://gw.alipayobjects.com/zos/rmsportal/TVirpZOSqZPiUqUDxSZa.svg');
    /* 可选：增加纹理感 */
    display: flex;
    flex-direction: column;
}

.content-container {
    flex: 1;
    display: flex;
    align-items: center;
    /* 垂直居中 */
    justify-content: center;
    /* 水平居中 */
    padding: 32px 0;
}

.login-card {
    width: 480px;
    /* 适中的卡片宽度 */
    background: #fff;
    padding: 40px 48px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    /* 柔和阴影 */
}

.header {
    text-align: center;
    margin-bottom: 40px;
}

.title {
    font-size: 30px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 12px;
}

.desc {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
}

.input-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.demo-actions {
    text-align: right;
    font-size: 12px;
    color: #bfbfbf;
}

.demo-actions a {
    color: #1890ff;
}

.tips {
    text-align: right;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
    margin-bottom: 12px;
}

.footer {
    padding: 24px;
    text-align: center;
    color: rgba(0, 0, 0, 0.45);
}

/* 强制对齐 Label */
:deep(.ant-form-item-label) {
    display: flex;
    align-items: flex-start;
    padding-top: 8px !important;
    justify-content: flex-end;
}

:deep(.ant-form-item-label > label) {
    font-weight: 500;
    font-size: 15px;
}
</style>