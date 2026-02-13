<template>
    <div>
        <Dialog
          :show="dialogConfig.show"
          :title="dialogConfig.title"
          :buttons="dialogConfig.buttons"
          width="500px"
          :show-cancel="false"
          @close="dialogConfig.show = false"
          >
        <el-form
          ref="formDataRef"
          :model="formData"
          label-width="80px"
          @submit.prevent
        >
        <!-- 头像上传修改 -->
        <el-form-item label="昵称">
            {{ formData.nickName }}
        </el-form-item>

        <el-form-item label="头像" prop="">
          <AvatarUpload v-model="formData.avatar"></AvatarUpload>
        </el-form-item>
       </el-form>
      </Dialog>
    </div>
</template>

<script setup>
import AvatarUpload from "@/components/AvatarUpload.vue";
import { ref, reactive, getCurrentInstance, nextTick } from "vue";
import { useUserInfoStore } from "@/stores/userInfoStore";
const { proxy } = getCurrentInstance();
const userInfoStore = useUserInfoStore();

const api = {
    updateUserAvatar: "updateUserAvatar",
};

const formData = ref({});
const formDataRef = ref();

const show = (data) => {
    formData.value = Object.assign({}, data);
    formData.value.avatar = { userId: data.userId, qqAvatar: data.avatar };
    dialogConfig.value.show = true;
};

defineExpose({ show });

const dialogConfig = ref({
    show: false,
    title: "修改头像",
    buttons: [
        {
            type: "primary",
            text: "确定",
            click: (e) => {
                submitForm();
            },
        },
    ],
});
const emit = defineEmits();
const submitForm = async () => {
    if (!(formData.value.avatar instanceof File)) {
        dialogConfig.value.show = false;
        return;
    }

    const result = await proxy.Request ({
        url: api.updateUserAvatar,
        params: {
            avatar: formData.value.avatar,
        },
    });
    if (!result) {
        return;
    }
    dialogConfig.value.show = false;
    const cookieUserInfo = userInfoStore.userInfo;
    delete cookieUserInfo.avatar;
    userInfoStore.setUserInfo(cookieUserInfo);
    emit("updateAvatar");
};
</script>

<style lang="scss" scoped>
</style>
