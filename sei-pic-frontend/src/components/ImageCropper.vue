<template>
    <a-modal class="image-cropper" v-model:open="visible" @cancel="closeModal" :footer="null">
        <vue-cropper ref="cropperRef" :img="internalImageUrl" :autoCrop="canEdit" :fixedBox="false" :centerBox="true"
            :infoTrue="true" :full="true" :canMoveBox="true" :info="true" outputType="png" />
        <div style="margin-bottom: 16px" />
        <!-- 图片操作 -->
        <div class="image-cropper-actions">
            <a-space>
                <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
                <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
                <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
                <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
                <a-button type="primary" :loading="loading" :disabled="!canEdit" @click="handleConfirm">
                    确认
                </a-button>
            </a-space>

        </div>

        <!-- 协同编辑操作 -->
        <div class="image-edit-actions" v-if="isTeamSpace">
            <a-space>
                <a-button v-if="editingUser" disabled> {{ editingUser.userName }}正在编辑</a-button>
                <a-button v-if="canEnterEdit" type="primary" ghost @click="enterEdit">进入编辑</a-button>
                <a-button v-if="canExitEdit" danger ghost @click="exitEdit">退出编辑</a-button>
            </a-space>
        </div>


    </a-modal>
</template>

<script setup lang="ts">
import { uploadPictureUsingPost } from '@/api/pictureController'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture'
import { SPACE_TYPE_ENUM } from '@/constants/space'
import { useLoginUserStore } from '@/stores/useLoginStore'
import PictureEditWebSocket from '@/utils'
import { message } from 'ant-design-vue'
import { computed, nextTick, onUnmounted, watch, watchEffect } from 'vue'
import { ref } from 'vue'

// #region 图片编辑
interface Props {
    imageUrl?: string
    picture?: API.PictureVO
    spaceId?: number
    space?: API.SpaceVO
    onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

// 编辑器组件的引用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
    cropperRef.value.rotateLeft()
    editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}

// 向右旋转
const rotateRight = () => {
    cropperRef.value.rotateRight()
    editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

// 缩放
const changeScale = (num: number) => {
    cropperRef.value.changeScale(num)
    if (num > 0) {
        editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_IN)
    } else {
        editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
    }
}



// 是否可见
const visible = ref(false)

// 打开弹窗
// 定义一个内部使用的图片地址变量
const internalImageUrl = ref<string>('')

// 修改 openModal 方法
const openModal = () => {
    // 方案：重新给图片地址赋值，触发组件刷新
    // 如果地址没变，vue-cropper 可能不会重置裁剪框，可以加个时间戳强制刷新（如果后端支持）
    // 或者先清空再赋值
    internalImageUrl.value = ''
    nextTick(() => {
        internalImageUrl.value = props.imageUrl || ''
        visible.value = true
    })
}


// // 关闭弹窗
// const closeModal = () => {
//     visible.value = false
// }

// 暴露函数给父组件
defineExpose({
    openModal,
})

const loading = ref<boolean>(false)

// 确认裁剪
const handleConfirm = () => {
    cropperRef.value.getCropBlob((blob: Blob) => {
        const fileName = (props.picture?.name || 'image') + '.png'
        const file = new File([blob], fileName, { type: blob.type })
        // 上传图片
        handleUpload({ file })
    })
}

/**
 * 上传
 * @param file
 */
const handleUpload = async ({ file }: any) => {
    loading.value = true
    try {
        const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
        params.spaceId = props.spaceId
        const res = await uploadPictureUsingPost(params, {}, file)
        if (res.data.code === 0 && res.data.data) {
            message.success('图片上传成功')
            // 将上传成功的图片信息传递给父组件
            props.onSuccess?.(res.data.data)
            closeModal();
        } else {
            message.error('图片上传失败，' + res.data.message)
        }
    } catch (error) {
        message.error('图片上传失败')
    } finally {
        loading.value = false
    }
}

// #endregion

// #region isTeamSpace
const isTeamSpace = computed(() => {
    return props.space?.spaceType === SPACE_TYPE_ENUM.TEAM
})
// #endregion


// #region 协同编辑
// --------- 实时编辑 ---------
const loginUserStore = useLoginUserStore()
let loginUser = loginUserStore.loginUser
// 正在编辑的用户
const editingUser = ref<API.UserVO>()
// 没有用户正在编辑中，可进入编辑
const canEnterEdit = computed(() => {
    return !editingUser.value
})
// 正在编辑的用户是本人，可退出编辑
const canExitEdit = computed(() => {
    return editingUser.value?.id === loginUser.id
})
// 可以编辑
const canEdit = computed(() => {
    // 不是团队空间，则默认可编辑
    if (!isTeamSpace.value) {
        return true
    }
    return editingUser.value?.id === loginUser.id
})


// #endregion

// #region websocket
let websocket: PictureEditWebSocket | null

// 初始化 WebSocket 连接，绑定事件
const initWebsocket = () => {
    const pictureId = props.picture?.id
    if (!pictureId || !visible.value) {
        return
    }
    // 防止之前的连接未释放
    if (websocket) {
        websocket.disconnect()
    }
    // 创建 WebSocket 实例
    websocket = new PictureEditWebSocket(pictureId)
    // 建立 WebSocket 连接
    websocket.connect()

    // 监听通知消息
    websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => {
        console.log('收到通知消息：', msg)
        message.info(msg.message)
    })

    // 监听错误消息
    websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => {
        console.log('收到错误消息：', msg)
        message.error(msg.message)
    })

    // 监听进入编辑状态消息
    websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
        console.log('收到进入编辑状态消息：', msg)
        message.info(msg.message)
        editingUser.value = msg.user
    })

    // 监听编辑操作消息
    websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
        console.log('收到编辑操作消息：', msg)
        message.info(msg.message)
        switch (msg.editAction) {
            case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
                cropperRef.value.rotateLeft()
                break
            case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
                cropperRef.value.rotateRight()
                break
            case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
                cropperRef.value.changeScale(1)
                break
            case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
                cropperRef.value.changeScale(-1)
                break
        }
    })

    // 监听退出编辑状态消息
    websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
        console.log('收到退出编辑状态消息：', msg)
        message.info(msg.message)
        editingUser.value = undefined
    })
}

// watchEffect(() => {
//     // 团队空间才初始化
//     if (isTeamSpace.value) {
//         initWebsocket()
//     }
// })

// 精确监听 visible 的变化
watch(
    () => visible.value,
    (newVisible) => {
        // 只有当弹窗变为可见，且是团队空间时，才初始化连接
        if (newVisible && isTeamSpace.value) {
            console.log('弹窗打开，开始初始化 WebSocket')
            initWebsocket()
        } else if (!newVisible) {
            // 弹窗关闭时，主动断开连接
            if (websocket) {
                websocket.disconnect()
                websocket = null // 清空引用
            }
            editingUser.value = undefined
        }
    }
)


onUnmounted(() => {
    // 断开连接
    if (websocket) {
        websocket.disconnect()
    }
    editingUser.value = undefined
})

// 关闭弹窗
const closeModal = () => {
    visible.value = false
    // 断开连接
    if (websocket) {
        websocket.disconnect()
    }
    editingUser.value = undefined
}

// 进入编辑状态
const enterEdit = () => {
    if (websocket) {
        // 发送进入编辑状态的消息
        websocket.sendMessage({
            type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT,
        })
    }
}

// 退出编辑状态
const exitEdit = () => {
    if (websocket) {
        // 发送退出编辑状态的消息
        websocket.sendMessage({
            type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT,
        })
    }
}

// 编辑图片操作
const editAction = (action: string) => {
    if (websocket) {
        // 发送编辑操作的请求
        websocket.sendMessage({
            type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
            editAction: action,
        })
    }
}


// #endregion



</script>

<style scoped>
.image-cropper {
    text-align: center;
}

.image-cropper .vue-cropper {
    height: 400px;
}

.image-edit-actions {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-top: 16px;
}
</style>
