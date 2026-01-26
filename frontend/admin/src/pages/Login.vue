<script setup lang="ts">
// 管理员登录页面，基于 Basic Auth 验证管理员账号。
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { login } from "../services/auth";
import { resolveErrorMessage } from "../services/errors";

const router = useRouter();
const route = useRoute();

const form = reactive({
  account: "",
  password: "",
});

const submitting = ref(false);

const handleSubmit = async () => {
  if (!form.account || !form.password) {
    ElMessage.warning("请输入账号和密码");
    return;
  }

  submitting.value = true;
  try {
    await login(form.account.trim(), form.password);
    ElMessage.success("登录成功");
    const redirect = route.query.redirect?.toString() || "/dashboard";
    router.replace(redirect);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "登录失败，请检查账号或密码"));
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="login-badge">管理控制台</div>
      <h2>管理端登录</h2>
      <p class="login-subtitle">浅色模式已启用，请使用管理员账号登录。</p>
      <el-form label-position="top" class="login-form">
        <el-form-item label="账号">
          <el-input
            v-model="form.account"
            placeholder="管理员账号"
            autocomplete="username"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="管理员密码"
            autocomplete="current-password"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-button type="primary" :loading="submitting" style="width: 100%;" @click="handleSubmit">
          登录
        </el-button>
      </el-form>
      <p class="login-hint">使用管理端基础认证账号登录（来自后台管理员表）。</p>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: radial-gradient(circle at top, rgba(255, 255, 255, 0.06), transparent 45%);
}

.login-card {
  width: min(400px, 92vw);
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: 18px;
  padding: 28px;
  box-shadow: var(--app-shadow);
}

.login-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--app-muted);
  margin-bottom: 12px;
}

.login-subtitle {
  margin: 0 0 16px;
  color: var(--app-muted);
  font-size: 13px;
}

.login-form :deep(.el-form-item__label) {
  color: var(--app-muted);
}

.login-hint {
  margin-top: 16px;
  color: var(--app-muted);
  font-size: 12px;
}
</style>
