<template>
    <div id="userEditPage">
        <a-card title="修改个人信息" :bordered="false" :style="{ maxWidth: '600px', margin: '0 auto' }">
            <a-form ref="formRef" :model="formData" :rules="rules" layout="vertical" @finish="handleSubmit">
                <a-form-item label="用户昵称" name="userName">
                    <a-input v-model:value="formData.userName" placeholder="请输入昵称" />
                </a-form-item>

                <a-form-item label="个人简介" name="userProfile">
                    <a-textarea v-model:value="formData.userProfile" placeholder="介绍一下你自己..." :rows="3" />
                </a-form-item>

                <!-- <a-divider>安全性修改（留空则不修改）</a-divider> -->

                <a-form-item label="原密码" name="oldPassword" extra="修改密码需先验证原密码">
                    <a-input-password v-model:value="formData.oldPassword" placeholder="请输入原密码" allow-clear />
                </a-form-item>

                <a-form-item label="新密码" name="userPassword" extra="8-16位，支持数字、字母、小数点">
                    <a-input-password v-model:value="formData.newPassWord" placeholder="请输入新密码" allow-clear />
                </a-form-item>

                <a-form-item>
                    <a-button type="primary" html-type="submit" :loading="submitting" block>
                        保存修改
                    </a-button>
                </a-form-item>
            </a-form>
        </a-card>
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { editUserUsingPost, getLoginUserUsingGet, updateUserUsingPost } from '@/api/userController';
import { useLoginUserStore } from '@/stores/useLoginStore';

const loginUserStore = useLoginUserStore();
const formRef = ref();
const submitting = ref(false);

// 表单数据定义
const formData = reactive({
    userName: '',
    userProfile: '',
    oldPassword: '', // 仅用于校验和传递给后端
    newPassWord: '', // 新密码
});

// 自定义密码格式校验逻辑
const validatePasswordFormat = async (_rule: Rule, value: string) => {
    if (!value) return Promise.resolve(); // 允许清空，清空不报错
    const reg = /^[a-zA-Z0-9.]{8,16}$/;
    if (!reg.test(value)) {
        return Promise.reject('密码必须为8-16位数字、字母或小数点');
    }
    return Promise.resolve();
};

// 互斥必填逻辑校验
const validatePasswordRelation = async (_rule: Rule, value: string) => {
    const { oldPassword, newPassWord } = formData;
    // 如果输入了其中一个，另一个必须有值
    if ((oldPassword && !newPassWord) || (!oldPassword && newPassWord)) {
        return Promise.reject('修改密码时，原密码与新密码均不能为空');
    }
    return Promise.resolve();
};

const rules: Record<string, Rule[]> = {
    userName: [{ max: 20, message: '昵称不能超过20字' }],
    userProfile: [{ max: 200, message: '简介不能超过200字' }],
    // 密码校验：组合格式校验与关联必填校验
    oldPassword: [
        { validator: validatePasswordRelation, trigger: 'blur' }
    ],
    userPassword: [
        { validator: validatePasswordFormat, trigger: 'change' },
        { validator: validatePasswordRelation, trigger: 'blur' }
    ],
};

/**
 * 初始化加载当前用户信息
 */
const loadData = async () => {
    const res = await getLoginUserUsingGet();
    if (res.data.code === 0 && res.data.data) {
        const user = res.data.data;
        formData.userName = user.userName ?? '';
        formData.userProfile = user.userProfile ?? '';
    }
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
    submitting.value = true;
    try {
        // 构造请求参数，处理“留空不更新”逻辑
        const updateRequest: API.UserEditRequest = {
            userName: formData.userName || undefined,
            userProfile: formData.userProfile || undefined,
            // 只有当两者都有值时才传递密码字段
            oldPassword: formData.oldPassword || undefined,
            newPassWord: formData.newPassWord || undefined,
        };

        const res = await editUserUsingPost(updateRequest);
        if (res.data.code === 0) {
            message.success('修改成功');
            // 如果修改了昵称等，更新全局 store
            loginUserStore.setLoginUser({
                ...loginUserStore.loginUser,
                userName: formData.userName,
                userProfile: formData.userProfile
            });
            // 修改密码后通常建议清空表单密码域
            formData.oldPassword = '';
            formData.newPassWord = '';
        } else {
            message.error('修改失败：' + res.data.message);
        }
    } catch (error) {
        console.error(error);
    } finally {
        submitting.value = false;
    }
};

onMounted(loadData);
</script>

<style scoped>
#userEditPage {
    padding-top: 40px;
}
</style>