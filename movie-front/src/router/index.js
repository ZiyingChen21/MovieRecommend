import { createRouter, createWebHistory } from 'vue-router'

// 定义页面路由(导航信息)
const routes = [
    {
        path: '/',
        redirect: '/login' // 默认跳转到登录页
    },
    {
        path: '/login',
        name: 'Login',
        // 懒加载：用到这个页面才会去加载对应的文件
        component: () => import('../views/Login.vue') 
    },
    {
        path: '/home',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        // 路由守卫：未登录不允许进入首页
        beforeEnter: (to, from, next) => {
            if (!localStorage.getItem('token')) {
                next('/login')
            } else {
                next()
            }
        }
    },
    {
        path: '/movie/:id',
        name: 'MovieDetail',
        component: () => import('../views/MovieDetail.vue'),
        beforeEnter: (to, from, next) => {
            if (!localStorage.getItem('token')) next('/login')
            else next()
        }
    },
    {
        path: '/library',
        name: 'Library',
        component: () => import('../views/Library.vue'),
        beforeEnter: (to, from, next) => {
            if (!localStorage.getItem('token')) next('/login')
            else next()
        }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router