// 管理端路由配置与鉴权守卫。
import { createRouter, createWebHistory } from "vue-router";
import { getAuthToken } from "../services/token";
import AdminLayout from "../layouts/AdminLayout.vue";
import Dashboard from "../pages/Dashboard.vue";
import Login from "../pages/Login.vue";
import Users from "../pages/Users.vue";
import Videos from "../pages/Videos.vue";
import Admins from "../pages/Admins.vue";

type RouteMetaConfig = {
  requiresAuth?: boolean;
  guestOnly?: boolean;
  title?: string;
};

const routes = [
  {
    path: "/login",
    component: Login,
    meta: { guestOnly: true, title: "管理员登录" } as RouteMetaConfig,
  },
  {
    path: "/",
    component: AdminLayout,
    meta: { requiresAuth: true } as RouteMetaConfig,
    children: [
      {
        path: "",
        redirect: "/dashboard",
      },
      {
        path: "dashboard",
        component: Dashboard,
        meta: { requiresAuth: true, title: "数据看板" } as RouteMetaConfig,
      },
      {
        path: "users",
        component: Users,
        meta: { requiresAuth: true, title: "用户管理" } as RouteMetaConfig,
      },
      {
        path: "videos",
        component: Videos,
        meta: { requiresAuth: true, title: "视频管理" } as RouteMetaConfig,
      },
      {
        path: "admins",
        component: Admins,
        meta: { requiresAuth: true, title: "管理员管理" } as RouteMetaConfig,
      },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/dashboard",
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const meta = to.meta as RouteMetaConfig;
  const hasAuth = Boolean(getAuthToken());

  if (meta.title) {
    document.title = `${meta.title} - 管理端`;
  }

  if (meta.requiresAuth && !hasAuth) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (meta.guestOnly && hasAuth) {
    return { path: "/dashboard" };
  }
  return true;
});

export default router;
