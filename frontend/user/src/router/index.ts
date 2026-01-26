// 用户端路由配置，控制登录态与页面跳转。
import { createRouter, createWebHistory } from "vue-router";
import { getToken } from "../services/token";
import Login from "../pages/Login.vue";
import Register from "../pages/Register.vue";
import Feed from "../pages/Feed.vue";
import Hot from "../pages/Hot.vue";
import Profile from "../pages/Profile.vue";
import VideoDetail from "../pages/VideoDetail.vue";

const routes = [
  { path: "/", redirect: () => (getToken() ? "/feed" : "/login") },
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/feed", component: Feed, meta: { requiresAuth: true } },
  { path: "/hot", component: Hot, meta: { requiresAuth: true } },
  { path: "/profile", component: Profile, meta: { requiresAuth: true } },
  { path: "/video/:id", component: VideoDetail, meta: { requiresAuth: true } },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const hasToken = Boolean(getToken());
  if (to.meta.requiresAuth && !hasToken) {
    return "/login";
  }
  if ((to.path === "/login" || to.path === "/register") && hasToken) {
    return "/feed";
  }
  return true;
});

export default router;
