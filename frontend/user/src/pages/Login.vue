<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { login } from "../services/auth";
import { setToken } from "../services/token";

const router = useRouter();
const account = ref("");
const password = ref("");
const errorMessage = ref("");
const loading = ref(false);

const submit = async () => {
  errorMessage.value = "";
  if (!account.value.trim() || !password.value.trim()) {
    errorMessage.value = "请输入账号和密码。";
    return;
  }

  loading.value = true;
  try {
    const token = await login({ account: account.value.trim(), password: password.value });
    if (!token) {
      throw new Error("Missing token");
    }
    setToken(token);
    router.push("/feed");
  } catch (error) {
    errorMessage.value = "登录失败，请检查账号或密码。";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <section class="auth-page">
    <div class="card auth-card stack">
      <div>
        <h1>欢迎回来</h1>
        <p class="muted">登录后继续观看。</p>
      </div>
      <label class="field">
        账号
        <input v-model="account" class="input" type="text" placeholder="邮箱或用户名" />
      </label>
      <label class="field">
        密码
        <input v-model="password" class="input" type="password" placeholder="密码" />
      </label>
      <button class="primary-button" type="button" :disabled="loading" @click="submit">
        {{ loading ? "登录中..." : "登录" }}
      </button>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <RouterLink to="/register" class="link-button">创建新账号</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 80px);
  display: grid;
  place-items: center;
  padding: 32px 16px;
}

.auth-card {
  width: min(420px, 100%);
}
</style>
