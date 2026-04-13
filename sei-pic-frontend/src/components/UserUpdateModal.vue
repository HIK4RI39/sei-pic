<template>
    <a-modal :visible="visible" title="更新用户信息" :confirm-loading="confirmLoading" @ok="handleOk" @cancel="handleCancel">
        <a-form :model="formState" layout="vertical" name="updateUserForm">
            <a-form-item label="用户名" name="userName">
                <a-input v-model:value="formState.userName" placeholder="请输入用户名" />
            </a-form-item>
            <a-form-item label="简介" name="userProfile">
                <a-textarea v-model:value="formState.userProfile" placeholder="请输入简介" :rows="4" />
            </a-form-item>
            <a-form-item label="角色" name="userRole">
                <a-select v-model:value="formState.userRole" placeholder="请选择角色">
                    <a-select-option value="user">用户</a-select-option>
                    <a-select-option value="admin">管理员</a-select-option>
                </a-select>
            </a-form-item>
            <a-form-item label="新密码 (不修改请留空)" name="userPassword">
                <a-input-password v-model:value="formState.userPassword" placeholder="请输入新密码" />
            </a-form-item>
        </a-form>
    </a-modal>
</template>

<script lang="ts" setup>
import { ref, watch, defineProps, defineEmits } from 'vue';
import { updateUserUsingPost } from '@/api/userController';
import { message } from 'ant-design-vue';

interface Props {
    visible: boolean;
    oldData: API.UserVO;
}

const props = defineProps<Props>();
// 定义触发事件
const emit = defineEmits(['update:visible', 'success']);

const confirmLoading = ref(false);
const formState = ref<API.UserUpdateRequest>({});

// 监听 visible，打开时拷贝数据
watch(
    () => props.visible,
    (newVal) => {
        if (newVal && props.oldData) {
            formState.value = {
                id: props.oldData.id,
                userName: props.oldData.userName,
                userProfile: props.oldData.userProfile,
                userRole: props.oldData.userRole,
            };
        }
    }
);

const handleOk = async () => {
    confirmLoading.value = true;
    try {
        const res = await updateUserUsingPost(formState.value);
        if (res.data.code === 0) {
            message.success('更新成功');
            emit('success'); // 通知父组件刷新列表
            emit('update:visible', false); // 通知父组件关闭弹窗
        } else {
            message.error('更新失败: ' + res.data.message);
        }
    } catch (e: any) {
        message.error('更新失败: ' + e.message);
    } finally {
        confirmLoading.value = false;
    }
};

const handleCancel = () => {
    // 核心：通过 emit 发送事件，让父组件去修改 visible 的值
    emit('update:visible', false);
};
</script>