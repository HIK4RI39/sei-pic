<template>
    <div id="userLoginPage">
        <h2 class="title">猫图库 - 用户登录</h2>
        <div class="desc">企业级智能协同云图库</div>
        <a-form :model="formState" @finish="handleLogin">
            <!-- username -->
            <a-form-item label="账号" name="userAccount" :rules="[
                { required: true, message: '账号是必填项' },
                { min: 4, max: 20, message: '账号长度必须在 4 到 20 个字符之间!', trigger: 'change' }
            ]">
                <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
            </a-form-item>
            <!-- pwd -->
            <a-form-item label="密码" name="userPassword" :rules="[
                { required: true, message: '密码是必填项' },
                { min: 8, max: 20, message: '密码长度必须在 8 到 20 个字符之间!', trigger: 'change' }
            ]">
                <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
            </a-form-item>
            <!-- register -->
            <div class="tips">
                没有账号?
                <RouterLink to="/user/register">立即注册</RouterLink>
            </div>
            <!-- submit -->
            <a-form-item>
                <a-button type="primary" style="width: 100%;" html-type="submit">登录</a-button>
            </a-form-item>
        </a-form>
    </div>
</template>

<script lang="ts" setup>
import { loginUsingPost } from '@/api/userController';
import { useLoginUserStore } from '@/stores/useLoginStore';
import { message } from 'ant-design-vue';
import { reactive } from 'vue';
import { useRouter } from 'vue-router';

const formState = reactive<API.UserLoginRequest>({});

const loginUserStore = useLoginUserStore();
const router = useRouter();

const handleLogin = async (values: any) => {
    const res = await loginUsingPost({
        userAccount: formState.userAccount,
        userPassword: formState.userPassword
    })
    try {
        if (res.data.code === 0 && res.data.data) {
            await loginUserStore.fetchLoginUser();
            message.success("登录成功")
            router.push({
                path: '/',
                replace: true
            })
        } else {
            message.error("登陆失败" + res.data.message)
        }
    } catch (e: any) {
        message.error("登陆失败" + e.message);
    }

};

</script>

<style scoped>
#userLoginPage {
    max-width: 360px;
    margin: 0 auto;
}

.title {
    text-align: center;
    margin-bottom: 16px;
}

.desc {
    color: #bbb;
    margin-bottom: 16px;
    text-align: center;
}

.tips {
    text-align: right;
    font-size: 13px;
    color: #bbb;
    margin-bottom: 16px;
}
</style>
