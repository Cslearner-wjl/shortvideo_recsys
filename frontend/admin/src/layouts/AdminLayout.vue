<script setup lang="ts">
// 管理端布局壳，提供侧边导航与顶栏操作。
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { logout } from "../services/auth";

const router = useRouter();
const route = useRoute();

const activeMenu = computed(() => {
  if (route.path.startsWith("/users")) {
    return "/users";
  }
  if (route.path.startsWith("/videos")) {
    return "/videos";
  }
  if (route.path.startsWith("/admins")) {
    return "/admins";
  }
  return "/dashboard";
});

const handleLogout = () => {
  logout();
  router.replace("/login");
};
</script>

<template>
  <el-container class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-logo">短视频管理端</div>
      <el-menu
        :default-active="activeMenu"
        router
        class="admin-menu"
      >
        <el-menu-item index="/dashboard">
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/videos">
          <span>视频管理</span>
        </el-menu-item>
        <el-menu-item index="/admins">
          <span>管理员管理</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <el-container class="admin-main">
      <el-header height="64px" class="admin-header">
        <div class="title">{{ route.meta.title ?? "管理控制台" }}</div>
        <el-button type="primary" plain size="small" @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main class="admin-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
