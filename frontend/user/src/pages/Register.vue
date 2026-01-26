<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";
import { useRouter } from "vue-router";
import { register, requestCode } from "../services/auth";

const router = useRouter();
const username = ref("");
const phone = ref("");
const email = ref("");
const password = ref("");
const emailCode = ref("");
const errorMessage = ref("");
const loading = ref(false);
const countdown = ref(0);
let timer: number | null = null;

const canRequestCode = computed(() => countdown.value === 0 && Boolean(email.value.trim()));

const startCountdown = () => {
  countdown.value = 60;
  timer = window.setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0 && timer) {
      window.clearInterval(timer);
      timer = null;
    }
  }, 1000);
};

const sendCode = async () => {
  errorMessage.value = "";
  if (!email.value.trim()) {
    errorMessage.value = "请输入邮箱。";
    return;
  }
  try {
    await requestCode({ email: email.value.trim() });
    startCountdown();
  } catch (error) {
    errorMessage.value = "验证码发送失败。";
  }
};

const submit = async () => {
  errorMessage.value = "";
  if (!username.value.trim() || !phone.value.trim() || !email.value.trim() || !password.value.trim() || !emailCode.value.trim()) {
    errorMessage.value = "请完整填写所有信息。";
    return;
  }
  loading.value = true;
  try {
    await register({
      username: username.value.trim(),
      phone: phone.value.trim(),
      email: email.value.trim(),
      password: password.value,
      emailCode: emailCode.value.trim(),
    });
    router.push("/login");
  } catch (error) {
    errorMessage.value = "注册失败。";
  } finally {
    loading.value = false;
  }
};

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer);
  }
});
</script>

<template>
  <section class="auth-page">
    <div class="card auth-card stack">
      <div>
        <h1>创建账号</h1>
        <p class="muted">开启你的专属推荐视频。</p>
      </div>
      <label class="field">
        用户名
        <input v-model="username" class="input" type="text" placeholder="用户名" />
      </label>
      <label class="field">
        手机号
        <input v-model="phone" class="input" type="tel" placeholder="11位手机号" />
      </label>
      <label class="field">
        邮箱
        <input v-model="email" class="input" type="email" placeholder="you@example.com" />
      </label>
      <div class="field">
        邮箱验证码
        <div class="code-row">
          <input v-model="emailCode" class="input" type="text" placeholder="验证码" />
          <button class="ghost-button" type="button" :disabled="!canRequestCode" @click="sendCode">
            {{ countdown ? `等待 ${countdown}s` : "获取验证码" }}
          </button>
        </div>
        <p class="muted hint">测试模式下验证码会打印在后端日志中。</p>
      </div>
      <label class="field">
        密码
        <input v-model="password" class="input" type="password" placeholder="密码" />
      </label>
      <button class="primary-button" type="button" :disabled="loading" @click="submit">
        {{ loading ? "注册中..." : "注册" }}
      </button>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <RouterLink to="/login" class="link-button">返回登录</RouterLink>
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
  width: min(460px, 100%);
}

.code-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.code-row .input {
  flex: 1;
}

.hint {
  margin: 8px 0 0;
  font-size: 12px;
}
</style>
