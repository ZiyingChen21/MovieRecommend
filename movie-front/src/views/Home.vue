<template>
  <el-container class="app-wrapper">
    <el-aside width="220px" class="sidebar">
      <div class="brand-logo" @click="clearSearch" style="cursor: pointer;">
        <span class="logo-icon">▶</span>
        <span class="logo-text">MOVIE ONLINE</span>
      </div>
      <el-menu default-active="1" class="side-menu" :router="false">
        <el-menu-item index="1" @click="clearSearch">
          <el-icon><HomeFilled /></el-icon>
          <span>首页推荐</span>
        </el-menu-item>
        <el-menu-item index="2" @click="$router.push('/library')">
          <el-icon><VideoCamera /></el-icon>
          <span>高分电影</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="top-header">
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索电影、导演、演员... (按回车搜索)"
            class="search-input"
            clearable
            @keyup.enter="handleSearch"  @clear="clearSearch" >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="user-actions">
          <el-dropdown>
            <div class="avatar-wrapper" style="display: flex; align-items: center; gap: 10px; cursor: pointer;">
              <span style="color: white; font-weight: bold;">{{ currentUsername }}</span>
              <el-avatar :size="36" :src="currentAvatar" style="background-color: #409eff;">
                {{ currentAvatar ? '' : currentUsername.charAt(0).toUpperCase() }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showAvatarDialog = true">更换专属头像</el-dropdown-item>
                <el-dropdown-item @click="openTagDialog">修改偏好标签</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content-area">
        <div v-if="isSearching" class="search-results-container">
        <div class="section-header">
          <h2 class="section-title">搜索结果: <span style="color: #E50914;">"{{ currentSearchText }}"</span></h2>
        </div>
        
        <el-skeleton :loading="searchLoading" animated>
          <template #default>
            <div class="movie-grid" v-if="searchResults.length > 0">
              <div class="movie-card-grid" v-for="movie in searchResults" :key="movie.mid" @click="$router.push(`/movie/${movie.mid}`)"> 
                <div class="image-wrapper">
                  <img :src="movie.image || defaultPoster" class="image" />
                  
                  <div class="overlay-info" v-if="movie.avgScore || movie.matchRate || movie.score">
                    <span class="score avg-score" v-if="movie.avgScore || movie.score">
                      ⭐ {{ movie.avgScore || movie.score }}
                    </span>
                    <span class="score-divider" v-if="(movie.avgScore || movie.score) && movie.matchRate">|</span>
                    <span class="score match-rate" v-if="movie.matchRate">
                      ✨ {{ Number(movie.matchRate).toFixed(1) }}%
                    </span>
                  </div>

                </div>  
                <div class="movie-info">
                  <span class="movie-name">{{ movie.name }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="没找到相关电影，换个词试试吧~" />
          </template>
        </el-skeleton>
      </div>

        <el-carousel height="450px" class="hero-banner" indicator-position="none" arrow="hover">
          <el-carousel-item v-for="(banner, index) in bannerList" :key="index">
            <div class="banner-item" :style="{ backgroundImage: 'url(' + banner.image + ')' }">
              <div class="banner-overlay">
                <div class="banner-content-box">
                  <h1 class="banner-title">{{ banner.title }}</h1>
                  <p class="banner-desc">{{ banner.desc }}</p>
                  <div class="banner-buttons">
                    <el-button size="large" class="play-btn" style="color: black; font-weight: bold; font-size: 16px; padding: 0 30px;" @click="handleQuickPlay(banner.title)">
                      <el-icon style="margin-right: 8px; font-size: 20px;"><VideoPlay /></el-icon> 播放
                    </el-button>
                    <el-button color="rgba(109, 109, 110, 0.7)" size="large" class="info-btn" style="color: white" @click="goToDetail(banner.mid)">
                      <el-icon style="margin-right: 5px;"><InfoFilled /></el-icon> 更多信息
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>


        <div class="section-container" v-if="recommendSections.history && recommendSections.history.movies.length > 0">
          <div class="section-header">
            <h2 class="section-title">
              {{ recommendSections.history.title }} 
              <span class="sub-title">{{ recommendSections.history.subTitle }}</span>
            </h2>
          </div>

          <el-skeleton :loading="recommendSections.history.loading" animated>
            <template #default>
              <div class="horizontal-scroll-wrapper">
                <div 
                  class="movie-card-horizontal" 
                  v-for="movie in recommendSections.history.movies" 
                  :key="movie.mid" 
                  @click="goToDetail(movie.mid)"
                >
                  <div class="image-wrapper">
                    <img 
                      :src="movie.image || defaultPoster" 
                      class="image" 
                      loading="lazy"
                      @error="(e) => { e.target.onerror = null; e.target.src = defaultPoster }"
                    />
                  </div>
                  <div class="movie-info">
                    <span class="movie-name">{{ movie.name }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
=
        <div class="section-container" v-for="(section, key) in recommendSections" :key="key" v-show="key !== 'history' && (section.loading || section.movies.length > 0)">
          <div class="section-header">
            <h2 class="section-title">{{ section.title }} <span class="sub-title" v-if="section.subTitle">{{ section.subTitle }}</span></h2>
            <div class="scroll-controls">
              <el-button circle size="small" @click="scrollRow(key, -1)"><el-icon><ArrowLeft /></el-icon></el-button>
              <el-button circle size="small" @click="scrollRow(key, 1)"><el-icon><ArrowRight /></el-icon></el-button>
            </div>
          </div>

          
          <el-skeleton :loading="section.loading" animated>
            <template #template>
              <div class="horizontal-scroll-wrapper">
                <div class="movie-card-horizontal" v-for="(movie, index) in section.movies" :key="movie.mid">
                  <el-skeleton-item variant="image" style="width: 100%; aspect-ratio: 2/3; border-radius: 6px;" />
                </div>
              </div>
            </template>
            
            <template #default>
              <div class="horizontal-scroll-wrapper" :ref="el => scrollRefs[key] = el">
                <div 
                  class="movie-card-horizontal" 
                  v-for="(movie, index) in section.movies" 
                  :key="movie.mid" 
                  @click="goToDetail(movie.mid)"
                >
                  <div class="rank-badge" v-if="key === 'hot'">{{ index + 1 }}</div>
                  
                  <div class="image-wrapper">
                    <img 
                      :src="movie.image || defaultPoster" 
                      class="image" 
                      loading="lazy"
                      @error="(e) => { e.target.onerror = null; e.target.src = defaultPoster }"
                    />
                    
                    <div class="overlay-info" v-if="movie.avgScore || movie.score || (key === 'feed' && movie.matchRate)">
                      <span class="score avg-score" v-if="movie.avgScore || movie.score">
                        ⭐ {{ movie.avgScore || movie.score }}
                      </span>
                      <span class="score-divider" v-if="(movie.avgScore || movie.score) && movie.matchRate && key === 'feed'">|</span>
                      <span class="score match-rate" v-if="movie.matchRate && key === 'feed'">
                        ✨ {{ Number(movie.matchRate).toFixed(1) }}%
                      </span>
                    </div>

                  </div>

                  <div class="movie-info">
                    <span class="movie-name" :title="movie.name">{{ movie.name }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </el-main>
    </el-container>

    <el-dialog
      v-model="showTagDialog"
      title="🎯 开启您的私人定制影院"
      width="500px"
      :close-on-click-modal="false" 
      :close-on-press-escape="false" 
      :show-close="false"
      destroy-on-close
    >
      <div class="tags-desc">选择至少 3 个标签，让我们为您精准推荐</div>
      <div class="tags-container">
        <el-check-tag
          v-for="tag in availableGenres"
          :key="tag"
          :checked="selectedGenres.includes(tag)"
          @change="toggleGenre(tag)"
          class="custom-tag"
        >
          {{ tag }}
        </el-check-tag>
      </div>
      <template #footer>
        <span class="dialog-footer">
          
          <el-button type="primary" @click="submitGenres" :disabled="selectedGenres.length < 3">
            开启推荐之旅 (已选 {{ selectedGenres.length }})
          </el-button>
        </span>
      </template>

    </el-dialog>

    <el-dialog v-model="showAvatarDialog" title="选择专属头像" width="450px" destroy-on-close>
      <div class="avatar-grid" style="display: flex; gap: 15px; flex-wrap: wrap; justify-content: center;">
        <div 
          v-for="(url, index) in presetAvatars" 
          :key="index"
          @click="tempSelectedAvatar = url"
          :style="{
            border: tempSelectedAvatar === url ? '3px solid #E50914' : '3px solid transparent',
            borderRadius: '50%', cursor: 'pointer', padding: '2px'
          }"
        >
          <el-avatar :size="60" :src="url" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showAvatarDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAvatar">确认更换</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { Search, HomeFilled, VideoCamera, DataLine, ArrowRight, ArrowLeft, CaretRight, InfoFilled, VideoPlay } from '@element-plus/icons-vue'
import defaultPosterImg from '/src/assets/default_poster.jpg'

const handleQuickPlay = (movieName) => {
  if (!movieName) return
  const keyword = `${movieName} 电影 预告`
  const url = `https://search.bilibili.com/all?keyword=${encodeURIComponent(keyword)}`
  window.open(url, '_blank')
}


const currentUsername = localStorage.getItem('currentUsername') || 'default'
const GENRE_STORAGE_KEY = `userGenres_${currentUsername}` 

const currentAvatar = ref(localStorage.getItem('userAvatar') || '');
const showAvatarDialog = ref(false);
const tempSelectedAvatar = ref('');

// 预设头像静态库 (可替换为项目 /src/assets 目录下的真实路径或图床URL)
const presetAvatars = [
  'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
  '/src/assets/1.jpg',
  '/src/assets/2.jpg',
  '/src/assets/3.jpg',
  '/src/assets/4.jpg',
];

// 提交逻辑
const submitAvatar = () => {
  if (!tempSelectedAvatar.value) return;
  
  request.post('/user/updateAvatar', { avatar: tempSelectedAvatar.value }).then(res => {
    ElMessage.success('头像更新成功');
    currentAvatar.value = tempSelectedAvatar.value;
    localStorage.setItem('userAvatar', tempSelectedAvatar.value); // 同步本地缓存
    showAvatarDialog.value = false;
  }).catch(() => {
    ElMessage.error('更新失败，请重试');
  });
};
// 跳转到电影详情页
const goToDetail = (mid) => {
  router.push(`/movie/${mid}`)
}

const defaultPoster = defaultPosterImg

const router = useRouter()
const searchKeyword = ref('')
const isSearching = ref(false)      // 是否处于搜索模式
const searchLoading = ref(false)    // 搜索加载骨架屏
const searchResults = ref([])       // 搜索结果数据
const currentSearchText = ref('')   // 当前正在展示的搜索词
 
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    clearSearch()
    return
  }
  
  isSearching.value = true
  searchLoading.value = true
  currentSearchText.value = keyword
  window.scrollTo({ top: 0, behavior: 'smooth' }) // 滚回顶部

  // 请求 Elasticsearch 接口
  request.get(`/search/text?query=${keyword}&size=24`).then(res => {
    searchResults.value = res.data || []
    sessionStorage.setItem('movie_search_state', JSON.stringify({
      keyword: keyword,
      results: searchResults.value
    }))
  }).catch(() => {
    ElMessage.error('搜索服务开小差了')
    searchResults.value = []
  }).finally(() => {
    searchLoading.value = false
  })
}

// 清除搜索，返回首页状态
const clearSearch = () => {
  isSearching.value = false
  searchKeyword.value = ''
  currentSearchText.value = '' 
  searchResults.value = []

  sessionStorage.removeItem('movie_search_state')
}

// 获取 DOM 元素的引用，用于实现左右点击滚动
const scrollRefs = reactive({})

// 第 0 排：巨幕轮播图数据
const bannerList = ref([
  {
    mid: 1001, 
    title: '流浪地球 2',
    desc: '人类的勇气与坚毅，将照亮宇宙的星空。',
    image: '/src/assets/earth2.png'
  },
  {
    mid: 1002, 
    title: '星际穿越 Interstellar',
    desc: '爱是唯一可以超越时间与空间的事物。',
    image: 'https://image.tmdb.org/t/p/original/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg'
  }
])

// 1. 定义 5 个独立的轨道
const recommendSections = reactive({
  history: { 
    title: '🕗 最近看过', 
    subTitle: '接着上次的精彩继续',
    movies: [], 
    loading: true 
  },feed: { // 核心主轨道：混合推荐流
    title: '✨ 猜您喜欢', 
    subTitle: '为您实时深度定制',
    movies: [], loading: true 
  },
  recent: { 
    title: '近期热搜', 
    subTitle: '大家最近都在看',
    movies: [], loading: true 
  },
  hot: { 
    title: '历史 Top10', 
    subTitle: '不可错过的经典',
    movies: [], loading: true 
  }
})

// 控制轨道的左右平滑滚动
const scrollRow = (key, direction) => {
  const container = scrollRefs[key]
  if (container) {
    const scrollAmount = direction * 800 // 每次滚动 800px
    container.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

// 标签选择逻辑
const showTagDialog = ref(false)
const availableGenres = ['Western','Crime', 'Drama', 'Thriller', 'Documentary', 'Romance', 'Music', 'Animation', 'War', 'Fantasy', 'Comedy', 'Adventure', 'Action', 'Mystery', 'Horror', 'Sci-Fi']
const selectedGenres = ref([])

const toggleGenre = (genre) => {
  const index = selectedGenres.value.indexOf(genre)
  if (index > -1) {
    selectedGenres.value.splice(index, 1)
  } else {
    selectedGenres.value.push(genre)
  }
}

// 标签提交逻辑：将用户选择保存到 LocalStorage
const submitGenres = () => {
  request.post('/user/updatePrefs', selectedGenres.value).then(res => {
    showTagDialog.value = false
    ElMessage.success('偏好同步云端成功，正在生成专属推荐...')
    
    // 后端保存成功后，再更新本地缓存
    localStorage.setItem(GENRE_STORAGE_KEY, JSON.stringify(selectedGenres.value))
    fetchAllData()
  }).catch(() => {
    ElMessage.error('偏好保存失败，请重试')
  })
}


// 并行获取后端真实数据
const fetchAllData = () => {
  // 从本地取出用户的偏好标签，用于给后端做冷启动降级
  const savedGenres = JSON.parse(localStorage.getItem(GENRE_STORAGE_KEY)) || ['Action']
  const targetGenre = savedGenres[0]

  // 请求最近浏览记录
  request.get('/user/history/list').then(res => {
    // 如果返回数据为空，可以直接把这个轨道隐藏或者显示占位
    recommendSections.history.movies = res.data || []
    recommendSections.history.loading = false
  }).catch(() => { 
    recommendSections.history.loading = false 
  })


  // 轨道 1：超级混合推荐流 (向后端新接口发请求，替代了之前的两次请求)
  request.get(`/recommend/feed?prefGenre=${targetGenre}`).then(res => {
    recommendSections.feed.movies = res.data || []
    recommendSections.feed.loading = false
  }).catch(() => { recommendSections.feed.loading = false })

  // 轨道 2：近期热门
  request.get('/recommend/recent').then(res => {
    recommendSections.recent.movies = res.data || []
    recommendSections.recent.loading = false
  }).catch(() => { recommendSections.recent.loading = false })

  // 轨道 3：历史热门 (配合前端的排行角标 UI)
  request.get('/recommend/hot').then(res => {
    // 强制只取前 10 名，保证 UI 完美
    recommendSections.hot.movies = (res.data || []).slice(0, 10)
    recommendSections.hot.loading = false
  }).catch(() => { recommendSections.hot.loading = false })


  // 轨道 4、5、6...：根据用户选择的标签，动态生成多个专属精选轨道
  savedGenres.forEach((genre) => {
    const trackKey = `genre_${genre}` // 给每个轨道一个唯一的 Key，比如 genre_Action
    
    // 1. 先在响应式对象中动态注册这个新轨道
    recommendSections[trackKey] = {
      title: `${genre} 迷精选`,
      subTitle: '为您专属定制的类别榜单',
      movies: [],
      loading: true
    }

    // 2. 异步请求该分类下的 Top N 数据
    request.get(`/recommend/genres?genre=${genre}`).then(res => {
      // 控制展示数量： 15 部
      recommendSections[trackKey].movies = (res.data || []).slice(0, 15)
      recommendSections[trackKey].loading = false
    }).catch(() => {
      recommendSections[trackKey].loading = false
    })
  })
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('currentUsername') // 清除用户名标识
  sessionStorage.removeItem('movie_search_state') 
  router.push('/login')
  ElMessage.success('已退出登录')
}

// 专门处理打开弹窗的逻辑，确保每次打开都是真实的本地状态
const openTagDialog = () => {
  const savedGenres = localStorage.getItem(GENRE_STORAGE_KEY)
  if (savedGenres) {
    // 老用户：重置为本地真实数据
    selectedGenres.value = JSON.parse(savedGenres)
  } else {
    // 新用户：清空状态
    selectedGenres.value = []
  }
  showTagDialog.value = true
}

onMounted(() => {
  // 1. 检查本地是否有偏好记录
  const savedGenres = localStorage.getItem(GENRE_STORAGE_KEY)
  if (!savedGenres) {
    // 2. 如果没选过，强制弹窗，且不执行 fetchAllData（因为没标签推荐不准）
    showTagDialog.value = true
  } else {
    // 这样下次打开弹窗时，之前选过的标签就会“一直亮着”
    selectedGenres.value = JSON.parse(savedGenres)
    fetchAllData()
  }
  //恢复搜索现场
  const savedState = sessionStorage.getItem('movie_search_state')
  if (savedState) {
    const state = JSON.parse(savedState)
    // 把之前存的数据重新赋给 Vue 的响应式变量
    searchKeyword.value = state.keyword
    currentSearchText.value = state.keyword
    searchResults.value = state.results
    isSearching.value = true // 强制开启搜索面板显示
  }
})
</script>

<style scoped>
.app-wrapper {
  height: 100vh;
  background-color: #141414; /* 经典深色沉浸模式底色 */
}

/* 左侧导航栏 */
.sidebar {
  background-color: #000000;
  border-right: 1px solid #333;
  display: flex;
  flex-direction: column;
}

.brand-logo {
  height: 70px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  color: #E50914; /* Netflix 红 */
  font-weight: 900;
  font-size: 20px;
  letter-spacing: 1px;
}
.logo-icon { margin-right: 8px; }

.side-menu {
  border-right: none;
  flex: 1;
  background-color: transparent;
}
.el-menu-item {
  color: #b3b3b3;
  border-radius: 4px;
  margin: 4px 12px;
  height: 48px;
  line-height: 48px;
  font-size: 15px;
}
.el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}
.el-menu-item.is-active {
  background-color: transparent;
  color: #fff;
  font-weight: bold;
  border-left: 3px solid #E50914; /* 活跃项左侧红条 */
}

/* 顶部区域 */
.top-header {
  background-color: transparent; /* 透明顶部 */
  background-image: linear-gradient(to bottom, rgba(0,0,0,0.7) 10%, rgba(0,0,0,0));
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 70px;
  position: absolute;
  top: 0;
  right: 0;
  left: 220px; /* 避开侧边栏 */
  z-index: 100;
}

.search-bar { width: 300px; }
:deep(.search-input .el-input__wrapper) {
  border-radius: 4px;
  background-color: rgba(0,0,0,0.75);
  box-shadow: 1px 1px 3px rgba(255,255,255,0.2) inset;
}
:deep(.search-input .el-input__inner) { color: white; }

/* 主内容区 */
.content-area {
  padding: 0; 
  overflow-y: auto;
  overflow-x: hidden;
}

/* 巨幕轮播图 */
.hero-banner {
  margin-bottom: 20px;
}
.banner-item {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center 20%;
  position: relative;
}
.banner-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 80%;
  background: linear-gradient(to top, #141414 0%, transparent 100%);
  display: flex;
  align-items: flex-end;
  padding: 0 40px 40px 40px;
  box-sizing: border-box;
}
.banner-content-box {
  width: 50%;
}
.banner-title {
  color: #fff;
  font-size: clamp(24px, 4vw, 48px);;
  font-weight: 900;
  margin: 0 0 15px 0;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.45);
}
.banner-desc {
  color: #fff;
  font-size: 18px;
  margin: 0 0 25px 0;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.45);
}
.play-btn {
  font-weight: bold;
  background-color: #fff;
  color: #000;
  border: none;
}
.play-btn:hover { background-color: rgba(255,255,255,0.75); }
.info-btn { border: none; font-weight: bold; }

/* 轨道容器 */
.section-container {
  padding: 0 40px;
  margin-bottom: 35px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 15px;
}
.section-title {
  font-size: 20px;
  font-weight: bold;
  color: #e5e5e5;
  margin: 0;
}
.sub-title {
  font-size: 14px;
  color: #808080;
  margin-left: 10px;
  font-weight: normal;
}
.scroll-controls .el-button {
  background-color: rgba(0,0,0,0.5);
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
}
.scroll-controls .el-button:hover {
  background-color: #fff;
  color: #000;
}

/* 核心：横向丝滑滚动 */
.horizontal-scroll-wrapper {
  display: flex;
  overflow-x: auto;
  gap: 15px; 
  padding-bottom: 10px;
  scroll-behavior: smooth;
  scroll-snap-type: x mandatory;
  align-items: flex-start; /* 防止卡片为了和旁边对齐而异常拉伸高度 */
}

/* 隐藏滚动条让视觉更干净 */
.horizontal-scroll-wrapper::-webkit-scrollbar {
  display: none;
}

.movie-card-horizontal {
  flex: 0 0 180px; 
  width: clamp(120px, 12vw, 200px); 
  max-width: 180px;  
  cursor: pointer;
  position: relative;
  scroll-snap-align: start;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 270px;      
  aspect-ratio: 2 / 3;
  border-radius: 4px;
  overflow: hidden;
  background-color: #222; 
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover; 
  transition: transform 0.3s ease;
}

.movie-card-horizontal:hover .image {
  transform: scale(1.05);
}


/* 热门榜的超大数字角标 */
.rank-badge {
  position: absolute;
  top: -10px;
  left: -10px;
  font-size: 60px;
  font-weight: 900;
  color: #000;
  -webkit-text-stroke: 2px #fff;
  z-index: 10;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
  pointer-events: none;
}

.overlay-info {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background-color: rgba(0, 0, 0, 0.85); /* 底色加深，磨砂质感 */
  backdrop-filter: blur(4px);
  padding: 4px 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.5);
  border: 1px solid rgba(255,255,255,0.1);
}

.score {
  font-weight: bold;
  font-size: 12px;
  display: flex;
  align-items: center;
}

/* 均分样式：经典的豆瓣黄 */
.avg-score {
  color: #F5C518; 
}

/* 分割线样式 */
.score-divider {
  color: #666;
  font-size: 10px;
}

/* 匹配度样式：科技感荧光绿 */
.match-rate {
  color: #46D369; 
  text-shadow: 0 0 5px rgba(70, 211, 105, 0.3); /* 轻微的发光效果 */
}

.movie-info { 
  padding-top: 8px; 
  width: 100%; /* 锁定文本区域宽度 */
  overflow: hidden;
}

.movie-name {
  font-size: 14px;
  color: #e5e5e5;
  font-weight: bold;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis; /* 文本超出显示省略号 */
  white-space: nowrap;     /* 绝对不允许文本换行撑开高度 */
}

/* 标签弹窗样式 */
.tags-desc { margin-bottom: 20px; color: #666; }
.tags-container { display: flex; flex-wrap: wrap; gap: 12px; }
.custom-tag { padding: 8px 16px; font-size: 14px; }


/* 搜索结果网格样式 */
.search-results-container {
  padding: 20px 0;
  min-height: calc(100vh - 70px);
}

.movie-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 25px 20px;
  margin-top: 20px;
}

.movie-card-grid {
  width: 100%;
  cursor: pointer;
}

.movie-card-grid .image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 3;
  border-radius: 4px;
  overflow: hidden;
  background-color: #222;
}

.movie-card-grid .image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.movie-card-grid:hover .image {
  transform: scale(1.05);
}

.movie-card-grid .movie-info {
  padding-top: 8px;
  width: 100%;
}

.movie-card-grid .movie-name {
  font-size: 14px;
  color: #e5e5e5;
  font-weight: bold;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 确保轨道容器有足够的空间 */
.section-container {
  padding: 0 40px;
  margin-bottom: 35px;
}

/* 核心：海报自适应 */
.movie-card-horizontal {
  /* 宽度根据屏幕自适应，最小120px，最大200px */
  flex: 0 0 clamp(120px, 12vw, 200px); 
  width: clamp(120px, 12vw, 200px);
  cursor: pointer;
}

.image-wrapper {
  width: 100%;
  aspect-ratio: 2 / 3; /* 锁定海报比例，高度自动计算 */
  border-radius: 6px;
  overflow: hidden;
  background-color: #222;
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保证图片不拉伸 */
  transition: transform 0.3s ease;
}

</style>