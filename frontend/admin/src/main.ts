// 管理端入口，挂载路由与全局样式。
import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "./styles/base.css";
import "./styles/layout.css";

const app = createApp(App);

app.use(ElementPlus);
app.use(router);
app.mount("#app");
