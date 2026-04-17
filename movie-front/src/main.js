import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css' // 引入 UI 样式

const app = createApp(App)

app.use(router) // 启用路由导航
app.use(ElementPlus) // 启用 UI 组件库
app.mount('#app')