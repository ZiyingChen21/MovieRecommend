<template>
  <el-container class="app-wrapper">
    <el-aside width="220px" class="sidebar">
      <div class="brand-logo" @click="$router.push('/home')" style="cursor: pointer;">
        <span class="logo-icon">▶</span>
        <span class="logo-text">MOVIE ONLINE</span>
      </div>
      <el-menu default-active="2" class="side-menu" :router="false">
        <el-menu-item index="1" @click="$router.push('/home')">
          <el-icon><HomeFilled /></el-icon>
          <span>首页推荐</span>
        </el-menu-item>
        <el-menu-item index="2">
          <el-icon><VideoCamera /></el-icon>
          <span>高分电影</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="top-header">
        <div class="header-left">
          <el-button link @click="$router.back()" class="back-btn">
            <el-icon style="font-size: 18px; margin-right: 5px;"><ArrowLeft /></el-icon> 返回
          </el-button>
          <span class="page-title">全部片库</span>
        </div>
      </el-header>

      <el-main class="content-area">
        <div class="filter-panel">
          <div class="filter-row">
            <span class="filter-label">全部类型</span>
            <div class="filter-options">
              <span 
                v-for="item in filters.genres" 
                :key="item"
                :class="['filter-item', { active: activeFilters.genre === item }]"
                @click="handleFilterChange('genre', item)"
              >
                {{ item }}
              </span>
            </div>
          </div>


          <div class="filter-row">
            <span class="filter-label">全部年份</span>
            <div class="filter-options">
              <span v-for="item in filters.years" :key="item"
                :class="['filter-item', { active: activeFilters.year === item }]"
                @click="handleFilterChange('year', item)">{{ item }}</span>
            </div>
          </div>

          <div class="filter-row">
            <span class="filter-label">综合排序</span>
            <div class="filter-options">
              <span 
                v-for="item in filters.sorts" 
                :key="item.value"
                :class="['filter-item', { active: activeFilters.sort === item.value }]"
                @click="handleFilterChange('sort', item.value)"
              >
                {{ item.label }}
              </span>
            </div>
          </div>
        </div>

        <el-skeleton :loading="loading" animated>
          <template #default>
            <div class="movie-grid" v-if="movieList.length > 0">
              <div class="movie-card-grid" v-for="movie in movieList" :key="movie.mid" @click="$router.push(`/movie/${movie.mid}`)"> 
                <div class="image-wrapper">
                  <img :src="movie.image || defaultPoster" class="image" @error="(e) => { e.target.onerror = null; e.target.src = defaultPoster }"  loading="lazy"/>
                  
                  <div class="overlay-info" v-if="movie.avgScore || movie.score">
                    <span class="score avg-score">
                      ⭐ {{ movie.avgScore || movie.score }}
                    </span>
                  </div>
                </div>  
                <div class="movie-info">
                  <span class="movie-name">{{ movie.name }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无符合条件的电影" />
            
            <div class="pagination-wrapper" v-if="total > 0">
              <el-pagination
                background
                layout="prev, pager, next"
                :total="total"
                :page-size="pageSize"
                v-model:current-page="currentPage"
                @current-change="handlePageChange"
              />
            </div>
          </template>
        </el-skeleton>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, VideoCamera, DataLine, ArrowLeft } from '@element-plus/icons-vue'
import request from '../utils/request'
import defaultPosterImg from '/src/assets/default_poster.jpg'

const router = useRouter()
const defaultPoster = defaultPosterImg

const loading = ref(true)
const movieList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(30)

// 筛选项配置字典
const filters = reactive({
  genres: ['全部', 'Western','Crime', 'Drama', 'Thriller', 'Documentary', 'Romance', 'Music', 'Animation', 'War', 'Fantasy', 'Comedy', 'Adventure', 'Action', 'Mystery', 'Horror', "Sci-Fi"],
  years: ['全部', '2016', '2015', '2014', '2013', '2000-2012', '更早'],
  sorts: [
    { label: '好评最高', value: 'rating' },
    { label: '热度最高', value: 'hot' },
    { label: '最新上线', value: 'issue' }
  ]
})




// 当前激活的筛选状态
const activeFilters = reactive({
  genre: '全部',
  year: '全部',
  sort: 'rating'
})

// 切换筛选条件
const handleFilterChange = (type, value) => {
  activeFilters[type] = value
  currentPage.value = 1 // 重置到第一页
  fetchData()
}

// 切换分页
const handlePageChange = (page) => {
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
  fetchData()
}

// 核心拉取数据逻辑
const fetchData = () => {
  loading.value = true
  
  // 组装请求参数
  const params = {
    page: currentPage.value,
    size: pageSize.value,
    sort: activeFilters.sort
  }
  

  if (activeFilters.genre !== '全部') {
    params.genre = activeFilters.genre
  }

  if (activeFilters.year !== '全部') {
    params.year = activeFilters.year
  }
 
  request.get('/movie/filter', { params }).then(res => {
    movieList.value = res.data.list || res.data || [] 
    total.value = res.data.total || 0 
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.app-wrapper { height: 100vh; background-color: #141414; }
.sidebar { background-color: #000; border-right: 1px solid #333; display: flex; flex-direction: column; }
.brand-logo { height: 70px; display: flex; align-items: center; padding: 0 20px; color: #E50914; font-weight: 900; font-size: 20px; }
.logo-icon { margin-right: 8px; }
.side-menu { border-right: none; flex: 1; background-color: transparent; }
.el-menu-item { color: #b3b3b3; margin: 4px 12px; border-radius: 4px; }
.el-menu-item:hover { background-color: rgba(255, 255, 255, 0.1); color: #fff; }
.el-menu-item.is-active { background-color: transparent; color: #fff; font-weight: bold; border-left: 3px solid #E50914; }

.top-header {
  background-color: #141414;
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  padding: 0 40px;
  height: 70px;
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.header-left { display: flex; align-items: center; gap: 20px; }
.back-btn { color: #aaa; font-size: 16px; font-weight: bold; }
.back-btn:hover { color: #fff; }
.page-title { color: #fff; font-size: 20px; font-weight: bold; }

.content-area {
  padding: 30px 40px;
  overflow-y: auto;
}

/* 筛选面板专属样式 */
.filter-panel {
  background-color: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
}
.filter-row:last-child { margin-bottom: 0; }

.filter-label {
  color: #888;
  width: 80px;
  flex-shrink: 0;
  line-height: 32px;
  font-size: 14px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-item {
  color: #ccc;
  font-size: 14px;
  line-height: 32px;
  padding: 0 16px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-item:hover { color: #fff; }

.filter-item.active {
  background-color: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-weight: bold;
}

/* 网格与卡片复用样式 */
.movie-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 30px 20px;
}

.movie-card-grid { width: 100%; cursor: pointer; }
.image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 3;
  border-radius: 6px;
  overflow: hidden;
  background-color: #222;
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.movie-card-grid:hover .image { transform: scale(1.05); }

.overlay-info {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background-color: rgba(0, 0, 0, 0.85);
  padding: 4px 8px;
  border-radius: 6px;
}
.avg-score { color: #F5C518; font-weight: bold; font-size: 12px; }

.movie-info { padding-top: 10px; }
.movie-name {
  font-size: 14px;
  color: #e5e5e5;
  font-weight: bold;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 分页器样式*/
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding-bottom: 20px;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #E50914;
}


</style>