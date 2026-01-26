<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { clearToken, tokenState } from "./services/token";

const route = useRoute();
const router = useRouter();
const hasToken = computed(() => Boolean(tokenState.value));

const logout = () => {
  clearToken();
  router.push("/login");
};

const isAuthPage = computed(
  () => route.path === "/login" || route.path === "/register"
);

const isFeedPage = computed(() => route.path === "/feed");
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand">短视频</div>
      <nav class="nav-links">
        <RouterLink to="/feed">推荐</RouterLink>
        <RouterLink to="/hot">榜单</RouterLink>
      </nav>
      <div class="nav-actions">
        <RouterLink v-if="!hasToken" to="/login" class="link-button">登录</RouterLink>
        <template v-else>
          <RouterLink to="/profile" class="link-button">我的</RouterLink>
          <button class="ghost-button" type="button" @click="logout">退出</button>
        </template>
      </div>
    </header>

    <main class="app-main" :class="{ padded: !isAuthPage && !isFeedPage }">
      <RouterView />
    </main>
  </div>
</template>
