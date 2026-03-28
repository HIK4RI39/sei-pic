<template>
    <div id="userRegisterPage">
        <h2 class="title">猫图库 - 用户注册</h2>
        <div class="desc">企业级智能协同云图库</div>
        <a-form :model="formState" @finish="handleRegister">
            <!-- username -->
            <a-form-item name="userAccount" :rules="[
                { required: true, message: '账号是必填项' },
                { min: 4, max: 20, message: '账号长度必须在 4 到 20 个字符之间!', trigger: 'blur' }
            ]">
                <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
            </a-form-item>
            <!-- pwd -->
            <a-form-item name="userPassword" :rules="[
                { required: true, message: '密码是必填项' },
                { min: 8, max: 20, message: '密码长度必须在 8 到 20 个字符之间!', trigger: 'change' }
            ]">
                <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
            </a-form-item>
            <!-- checkPwd -->
            <a-form-item name="checkPassword" :rules="[
                { required: true, message: '确认密码是必填项' },
                { min: 8, max: 20, message: '确认密码长度必须在 8 到 20 个字符之间!', trigger: 'change' },
                { validator: validCheckPassword, trigger: 'change' }
            ]">
                <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码" />
            </a-form-item>
            <!-- register -->
            <div class="tips">
                已有账号?
                <RouterLink to="/user/login">立即登录</RouterLink>
            </div>
            <!-- submit -->
            <a-form-item>
                <a-button type="primary" style="width: 100%;" html-type="submit">注册</a-button>
            </a-form-item>
        </a-form>
    </div>
</template>

<script lang="ts" setup>
import { registerUsingPost } from '@/api/userController';
import { message } from 'ant-design-vue';
import { reactive } from 'vue';
import { useRouter } from 'vue-router';

const formState = reactive<API.UserRegisterRequest>({});
const router = useRouter();

const validCheckPassword = async (_rule, value) => {
    if (value === '') {
        return Promise.reject('请再次输入密码');
    } else if (value !== formState.userPassword) {
        return Promise.reject("两次输入密码不一致!");
    } else {
        return Promise.resolve();
    }
};

const handleRegister = async (values: any) => {
    const res = await registerUsingPost({
        userAccount: formState.userAccount,
        userPassword: formState.userPassword,
        checkPassword: formState.checkPassword
    })
    try {
        if (res.data.code === 0 && res.data.data) {
            message.success("注册成功")
            router.push({
                path: '/user/login'
            })
        } else {
            message.error("注册失败," + res.data.message)
        }
    } catch (e: any) {
        message.error("注册失败," + e.message);
    }

};

</script>

<style scoped>
#userRegisterPage {
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
