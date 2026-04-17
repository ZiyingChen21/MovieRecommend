<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2 class="title">{{ isLogin ? '账号密码登录' : '注册新账号' }}</h2>
      
      <el-form :model="formData" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="formData.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        
        <el-form-item label="密码">
          <el-input :type="pwdType" v-model="formData.password" placeholder="请输入密码">
            <template #suffix>
              <el-icon 
                style="cursor: pointer; font-size: 16px; margin-top: 8px;"
                @mousedown="pwdType = 'text'"
                @mouseup="pwdType = 'password'"
                @mouseleave="pwdType = 'password'"
                @touchstart.prevent="pwdType = 'text'"
                @touchend.prevent="pwdType = 'password'"
              >
                <View />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="确认密码" v-if="!isLogin">
          <el-input :type="confirmPwdType" v-model="formData.confirmPassword" placeholder="请再次输入密码">
            <template #suffix>
              <el-icon 
                style="cursor: pointer; font-size: 16px; margin-top: 8px;"
                @mousedown="confirmPwdType = 'text'"
                @mouseup="confirmPwdType = 'password'"
                @mouseleave="confirmPwdType = 'password'"
                @touchstart.prevent="confirmPwdType = 'text'"
                @touchend.prevent="confirmPwdType = 'password'"
              >
                <View />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="isLogin">
          <el-checkbox v-model="formData.rememberMe">记住账号和密码</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" class="submit-btn">
            {{ isLogin ? '登 录' : '注 册' }}
          </el-button>
        </el-form-item>
        
        <div class="toggle-text">
          <el-link type="primary" :underline="false" @click="toggleMode">
            {{ isLogin ? '没有账号？点击注册' : '已有账号？返回登录' }}
          </el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue' 
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { View } from '@element-plus/icons-vue' 

const router = useRouter()
const isLogin = ref(true)
const pwdType = ref('password')
const confirmPwdType = ref('password')


const formData = ref({
  username: '',
  password: '',
  confirmPassword: '',
  rememberMe: false 
})


onMounted(() => {
  const savedLoginInfo = localStorage.getItem('loginInfo')
  if (savedLoginInfo) {
    const info = JSON.parse(savedLoginInfo)
    formData.value.username = info.username
    formData.value.password = info.password
    formData.value.rememberMe = true
  }
})

const toggleMode = () => {
  isLogin.value = !isLogin.value
  formData.value = { username: '', password: '', confirmPassword: '', rememberMe: false }
  pwdType.value = 'password'
  confirmPwdType.value = 'password'
}

const submitForm = () => {
  if (!formData.value.username || !formData.value.password) {
    ElMessage.warning('用户名和密码不能为空！')
    return
  }

  if (isLogin.value) {
    // 【登录逻辑】
    request.post('/user/login', {
      username: formData.value.username,
      password: formData.value.password,
      rememberMe: formData.value.rememberMe 
    }).then(res => {
      ElMessage.success('登录成功，欢迎回来！')
      const token = res.map ? res.map.token : res.token 
      localStorage.setItem('token', token)
      
  
      localStorage.setItem('currentUsername', formData.value.username)
      
      
      const userInfo = res.data || res.map?.data || res
      const GENRE_STORAGE_KEY = `userGenres_${formData.value.username}`
      
      if (userInfo && userInfo.prefGenres && userInfo.prefGenres.length > 0) {
        // 如果后端数据库里有 user 以前选过的标签，立刻恢复到本地缓存！
        localStorage.setItem(GENRE_STORAGE_KEY, JSON.stringify(userInfo.prefGenres))
      } else {
        // 如果后端没有（纯新用户），清空本地对应缓存，强制触发 Home.vue 的冷启动弹窗
        localStorage.removeItem(GENRE_STORAGE_KEY)
      }

      if (formData.value.rememberMe) {
        localStorage.setItem('loginInfo', JSON.stringify({
          username: formData.value.username,
          password: formData.value.password
        }))
      } else {
        localStorage.removeItem('loginInfo')
      }
    
      router.push('/home')
    })
  } else {
    // 【注册逻辑】
    if (formData.value.password !== formData.value.confirmPassword) {
      ElMessage.error('两次输入的密码不一致！')
      return
    }
    request.post('/user/register', {
      username: formData.value.username,
      password: formData.value.password,
      confirmPassword: formData.value.confirmPassword
    }).then(res => {
      ElMessage.success('注册成功！请登录')
      isLogin.value = true 
    })
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url('../assets/login_background.jpg'); 
  background-repeat: no-repeat;
  background-size: cover;
  background-position: center;
}

.login-box {
  width: 400px;
  padding: 20px;
  border-radius: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  background-color: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(6px);  
  -webkit-backdrop-filter: blur(6px);
  border: 1px solid rgba(255,255,255,0.2);
  animation: loginBoxIn 1s ease-out;
  transition: all 0.3s ease;
}

.login-box:hover {
  transform: translateY(-8px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #061843;
}

.submit-btn {
  width: 100%;
  font-size: 16px;
}

.toggle-text {
  text-align: right;
  margin-top: 10px;
}

@keyframes loginBoxIn {
  0% {
    opacity: 0;
    transform: translateY(40px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>