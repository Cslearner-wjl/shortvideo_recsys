<script setup lang="ts">
// 个人资料页面，支持头像与简介维护。
import { onMounted, reactive, ref } from "vue";
import { request } from "../services/http";

const loading = ref(true);
const saving = ref(false);
const changingPassword = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

const user = reactive({
  id: 0,
  username: "",
  phone: "",
  email: "",
  avatarUrl: "",
  bio: "",
  status: 1,
});

const editForm = reactive({
  username: "",
  phone: "",
  email: "",
  avatarUrl: "",
  bio: "",
});

const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const showPasswordModal = ref(false);

const loadProfile = async () => {
  loading.value = true;
  errorMessage.value = "";
  try {
    const resp = await request("/api/users/me");
    const data = resp?.data?.user || resp?.data || resp?.user || resp;
    user.id = data.id || 0;
    user.username = data.username || "";
    user.phone = data.phone || "";
    user.email = data.email || "";
    user.avatarUrl = data.avatarUrl || "";
    user.bio = data.bio || "";
    user.status = data.status ?? 1;

    editForm.username = user.username;
    editForm.phone = user.phone;
    editForm.email = user.email;
    editForm.avatarUrl = user.avatarUrl;
    editForm.bio = user.bio;
  } catch (error: any) {
    errorMessage.value = error?.message || "加载个人信息失败";
  } finally {
    loading.value = false;
  }
};

const saveProfile = async () => {
  saving.value = true;
  errorMessage.value = "";
  successMessage.value = "";
  try {
    const resp = await request("/api/users/me", {
      method: "PUT",
      body: {
        username: editForm.username || undefined,
        phone: editForm.phone || undefined,
        email: editForm.email || undefined,
        avatarUrl: editForm.avatarUrl || undefined,
        bio: editForm.bio || undefined,
      },
    });
    const data = resp?.data?.user || resp?.data || resp?.user || resp;
    user.username = data.username || user.username;
    user.phone = data.phone || user.phone;
    user.email = data.email || user.email;
    user.avatarUrl = data.avatarUrl || user.avatarUrl;
    user.bio = data.bio || user.bio;
    successMessage.value = "保存成功";
    setTimeout(() => (successMessage.value = ""), 3000);
  } catch (error: any) {
    errorMessage.value = error?.message || "保存失败";
  } finally {
    saving.value = false;
  }
};

const openPasswordModal = () => {
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  showPasswordModal.value = true;
};

const closePasswordModal = () => {
  showPasswordModal.value = false;
};

const changePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    errorMessage.value = "请填写完整";
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    errorMessage.value = "两次输入的新密码不一致";
    return;
  }
  if (passwordForm.newPassword.length < 6) {
    errorMessage.value = "新密码至少6位";
    return;
  }

  changingPassword.value = true;
  errorMessage.value = "";
  try {
    await request("/api/users/me/password", {
      method: "POST",
      body: {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      },
    });
    successMessage.value = "密码修改成功";
    closePasswordModal();
    setTimeout(() => (successMessage.value = ""), 3000);
  } catch (error: any) {
    errorMessage.value = error?.message || "密码修改失败";
  } finally {
    changingPassword.value = false;
  }
};

onMounted(() => {
  loadProfile();
});
</script>

<template>
  <section class="profile-page">
    <div class="profile-card">
      <h1 class="section-title">个人中心</h1>

      <p v-if="loading" class="muted">加载中...</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>

      <div v-if="!loading" class="profile-form">
        <div class="avatar-row">
          <div class="avatar-preview">
            <img v-if="editForm.avatarUrl" :src="editForm.avatarUrl" alt="avatar" />
            <span v-else>{{ editForm.username ? editForm.username.slice(0, 1).toUpperCase() : "U" }}</span>
          </div>
          <div class="field grow">
            <label>头像地址</label>
            <input v-model="editForm.avatarUrl" class="input" type="text" placeholder="https://..." />
          </div>
        </div>

        <div class="field">
          <label>用户名</label>
          <input v-model="editForm.username" class="input" type="text" placeholder="用户名" />
        </div>

        <div class="field">
          <label>手机号</label>
          <input v-model="editForm.phone" class="input" type="tel" placeholder="手机号" />
        </div>

        <div class="field">
          <label>邮箱</label>
          <input v-model="editForm.email" class="input" type="email" placeholder="邮箱" />
        </div>

        <div class="field">
          <label>个人简介</label>
          <textarea v-model="editForm.bio" class="input textarea" rows="3" placeholder="简单介绍一下自己"></textarea>
        </div>

        <div class="profile-actions">
          <button class="primary-button" :disabled="saving" @click="saveProfile">
            {{ saving ? "保存中..." : "保存修改" }}
          </button>
          <button class="ghost-button" @click="openPasswordModal">修改密码</button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordModal" class="modal-backdrop">
      <div class="modal">
        <div class="modal-title">修改密码</div>
        <div class="modal-body">
          <div class="field">
            <label>原密码</label>
            <input v-model="passwordForm.oldPassword" class="input" type="password" placeholder="请输入原密码" />
          </div>
          <div class="field">
            <label>新密码</label>
            <input v-model="passwordForm.newPassword" class="input" type="password" placeholder="请输入新密码（至少6位）" />
          </div>
          <div class="field">
            <label>确认新密码</label>
            <input v-model="passwordForm.confirmPassword" class="input" type="password" placeholder="请再次输入新密码" />
          </div>
        </div>
        <div class="modal-actions">
          <button class="ghost-button" @click="closePasswordModal">取消</button>
          <button class="primary-button" :disabled="changingPassword" @click="changePassword">
            {{ changingPassword ? "提交中..." : "确认修改" }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px;
}

.profile-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 24px;
  box-shadow: var(--shadow);
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.avatar-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.avatar-preview {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #efece6;
  color: #8d857b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.grow {
  flex: 1;
}

.profile-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.success {
  color: #059669;
  font-size: 14px;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.textarea {
  resize: vertical;
  min-height: 80px;
}
</style>
