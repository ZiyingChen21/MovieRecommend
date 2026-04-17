<template>
  <div class="detail-container">
    <el-page-header @back="goBack" class="header">
      <template #content>
        <div style="display: flex; align-items: center; gap: 15px;">
          <span class="header-title">电影详情</span>
          <el-button size="small" round icon="HomeFilled" @click="router.push('/home')" style="background: rgba(255,255,255,0.1); border: 1px solid #444; color: #eee;">
            返回主页
          </el-button>
        </div>
      </template>
    </el-page-header>
    
    <el-skeleton :loading="loading" animated style="padding: 0 40px;">
      <template #template>
        <div style="display: flex; gap: 40px; margin-bottom: 40px;">
          <el-skeleton-item variant="image" style="width: 300px; height: 450px; border-radius: 8px;" />
          <div style="flex: 1;">
            <el-skeleton-item variant="h1" style="width: 50%; margin-bottom: 20px;" />
            <el-skeleton-item variant="text" style="width: 30%; margin-bottom: 30px;" />
            <el-skeleton-item variant="text" style="width: 100%;" v-for="i in 5" :key="i" />
          </div>
        </div>
      </template>
      
      <template #default>
        <div class="content-area" v-if="movieInfo">
          <div class="movie-hero">
            <div class="poster-box">
              <img :src="movieInfo.image || defaultPoster" @error="(e) => { e.target.onerror = null; e.target.src = defaultPoster }" loading="lazy" class="poster" />
            </div>
            
            <div class="info-box">
              <div style="display: flex; align-items: flex-end; gap: 20px; margin-bottom: 15px;">
                <h1 class="movie-title" style="margin: 0;">{{ movieInfo.name }}</h1>
                
                <div class="detail-scores" v-if="movieInfo.avgScore || movieInfo.matchRate">
                  <span class="score-item avg" v-if="movieInfo.avgScore">
                    <span class="score-val">⭐ {{ movieInfo.avgScore }}</span>
                    <span class="score-label">大众评分</span>
                  </span>
                  <span class="score-item match" v-if="movieInfo.matchRate">
                    <span class="score-val">✨ {{ Number(movieInfo.matchRate).toFixed(1) }}%</span>
                    <span class="score-label">个性匹配</span>
                  </span>
                </div>
              </div>
              <div class="meta-data">
                <el-tag type="danger" effect="dark" round class="meta-tag" v-if="movieInfo.issue">{{ movieInfo.issue.substring(0, 4) }}</el-tag>
                <el-tag type="info" effect="dark" round class="meta-tag" v-if="movieInfo.timelong">{{ movieInfo.timelong }}</el-tag>
                <el-tag type="warning" effect="dark" round class="meta-tag" v-if="movieInfo.language">{{ movieInfo.language }}</el-tag>
              </div>

              <div class="genres">
                <el-tag 
                  v-for="genre in (movieInfo.genres ? movieInfo.genres.split('|') : [])" 
                  :key="genre" 
                  color="rgba(255,255,255,0.1)" 
                  style="border: 1px solid rgba(255,255,255,0.2); color: #ccc; margin-right: 10px;"
                >
                  {{ genre }}
                </el-tag>
              </div>

              <div class="action-buttons" style="margin-bottom: 25px; margin-top: 10px;">
                <el-button type="danger" size="large" class="main-play-btn" @click="handlePlay">
                  <el-icon style="margin-right: 8px; font-size: 20px;"><VideoPlay /></el-icon>
                  播放预告 / 全片
                </el-button>
              </div>

              <div class="detail-item">
                <span class="label">导演：</span>
                <span class="value">{{ movieInfo.directors ? movieInfo.directors.replace(/\|/g, ', ') : '未知' }}</span>
              </div>
              
              <div class="detail-item">
                <span class="label">主演：</span>
                <span class="value">{{ movieInfo.actors ? movieInfo.actors.replace(/\|/g, ', ') : '未知' }}</span>
              </div>

              <p class="descri">
                <span class="label">简介：</span>
                {{ movieInfo.descri || '暂无简介' }}
              </p>

              <div class="interactive-section">
                <div class="rating-box">
                  <span class="label">我的评分：</span>
                  <el-rate v-model="userRating" @change="handleRateChange" />
                  <span class="hint">{{ isRated ? '(已评分)' : '(尚未评分)' }}</span>
                </div>

                <div class="tag-cloud">
                  <span class="label">标签云：</span>
                  <div class="tags-wrapper">
                    <el-tag
                      v-for="tagName in allTags"
                      :key="tagName"
                      :type="myTags.has(tagName) ? 'danger' : 'info'" 
                      :effect="myTags.has(tagName) ? 'dark' : 'plain'"
                      class="interactive-tag"
                      @click="handleTagToggle(tagName)"
                    >
                      {{ tagName }}
                    </el-tag>
                    
                    <el-input
                      v-model="newTagValue"
                      placeholder="自定义标签..."
                      size="small"
                      style="width: 140px; margin-left: 10px;"
                      @keyup.enter="handleInputConfirm"
                    >
                      <template #append>
                        <el-button @click="handleInputConfirm"><el-icon><Plus /></el-icon></el-button>
                      </template>
                    </el-input>
                  </div>
                </div>
              </div>
              </div> </div> 
              <div class="recommend-section" v-if="similarMovies && similarMovies.length > 0">
            <div class="section-header">
              <h2 class="section-title">喜欢这部电影的人也喜欢</h2>
              <div class="scroll-controls">
                <el-button circle size="small" @click="scrollSimilar(-1)"><el-icon><ArrowLeft /></el-icon></el-button>
                <el-button circle size="small" @click="scrollSimilar(1)"><el-icon><ArrowRight /></el-icon></el-button>
              </div>
            </div>
            
            <div class="horizontal-scroll-wrapper" ref="similarScrollRef">
              <div class="movie-card-horizontal" v-for="movie in similarMovies" :key="movie.mid" @click="goToDetail(movie.mid)">
                <div class="image-wrapper">
                  <img :src="movie.image || defaultPoster" loading="lazy" @error="(e) => { e.target.onerror = null; e.target.src = defaultPoster }" class="image" />
                  
                  <div class="overlay-info" v-if="movie.avgScore || movie.score">
                    <span class="score avg-score">⭐ {{ movie.avgScore || movie.score }}</span>
                  </div>
                </div>
                
                <div class="movie-name-container">
                  <span class="movie-name" :title="movie.name">{{ movie.name }}</span>
                </div>
              </div>
            </div>
          </div>
            </div> </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { Plus, ArrowLeft, ArrowRight , VideoPlay} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const movieInfo = ref(null)
const similarMovies = ref([])
const defaultPoster = 'https://via.placeholder.com/300x450/222222/999999?text=No+Poster'
const userRating = ref(0) 
const isRated = ref(false)
const allTags = ref([])        // 这部电影下所有用户打的唯一标签名列表
const myTags = ref(new Set())  // 当前登录用户打过的标签名集合 (用于颜色高亮判断)
const newTagValue = ref('')    // 输入框绑定的值

const similarScrollRef = ref(null)
const scrollSimilar = (direction) => {
  if (similarScrollRef.value) {
    // 每次滑动 600px，behavior: 'smooth' 保证丝滑过渡
    similarScrollRef.value.scrollBy({ left: direction * 600, behavior: 'smooth' })
  }
}


const handlePlay = () => {
  if (!movieInfo.value || !movieInfo.value.name) return
  
  // 组装搜索词
  const keyword = `${movieInfo.value.name} 电影 预告`
  
  // 跳 Bilibili 
  const url = `https://search.bilibili.com/all?keyword=${encodeURIComponent(keyword)}`
  
  

  // 跳 豆瓣电影 (查影评)
  // const url = `https://search.douban.com/movie/subject_search?search_text=${encodeURIComponent(movieInfo.value.name)}`

 
  window.open(url, '_blank')
  ElMessage.success('正在为您寻找最佳播放源...')
}

// 获取电影详情及关联数据 (主入口)
const fetchDetailData = (mid) => {
  loading.value = true
  request.get(`/movie/info/${mid}`).then(res => {
    movieInfo.value = res.data
    // 获取相似推荐
    return request.get(`/recommend/similar?mid=${mid}`)
  }).then(res => {
    similarMovies.value = res.data || []
    // 同步加载用户个人的交互数据
    fetchMyRating(mid)
    fetchTagsData(mid)
    loading.value = false
  }).catch(err => {
    console.error('加载详情失败', err)
    loading.value = false
  })
}

// 获取评分状态
const fetchMyRating = (mid) => {
  request.get(`/rating/my/${mid}`).then(res => {
    if (res.data) {
      userRating.value = res.data.score 
      isRated.value = true
    } else {
      userRating.value = 0
      isRated.value = false
    }
  })
}

// 获取标签云及打标状态
const fetchTagsData = (mid) => {
  request.get(`/tag/my/${mid}`).then(res => {
    const myTagsList = res.data || []
    myTags.value = new Set(myTagsList.map(t => t.tag))
    
    return request.get(`/tag/movie/${mid}`)
  }).then(res => {
    const userTags = res.data || []
    const userTagNames = userTags.map(t => t.tag)
    
    
    const officialGenres = movieInfo.value.genres ? movieInfo.value.genres.split('|') : []
    
    // 使用 Set 去重，确保官方标签和用户新打的标签合并成唯一的展示列表
    const combinedTags = [...new Set([...officialGenres, ...userTagNames])]
    allTags.value = combinedTags
  })
}

// 处理评分修改
const handleRateChange = (val) => {
  const mid = route.params.id
  request.post(`/rating/add`, { mid: Number(mid), score: val }).then(() => {
    ElMessage.success('评分成功，推荐引擎已记录！')
    isRated.value = true
    // 实时刷新相似推荐
    request.get(`/recommend/similar?mid=${mid}`).then(res => {
      similarMovies.value = res.data || []
    })
  })
}


// 标签开关逻辑
const handleTagToggle = (tagName) => {
  const mid = route.params.id
  
  if (myTags.value.has(tagName)) {
    // 如果已高亮，说明打过，点一下执行取消（删除记录）
    request.delete(`/tag/remove/${mid}?tag=${encodeURIComponent(tagName)}`).then(() => {
      ElMessage.warning('已取消标签')
      fetchTagsData(mid) 
    })
  } else {
    // 如果是灰色，点一下执行添加（产生记录）
    submitTagAction(tagName)
  }
}

// 提交新标签
const submitTagAction = (tagName) => {
  if (!tagName) return
  const mid = route.params.id
  request.post(`/tag/add/${mid}?tag=${encodeURIComponent(tagName)}`).then(() => {
    ElMessage.success('打标成功！')
    newTagValue.value = ''
    fetchTagsData(mid)
  })
}

// 处理输入框提交
const handleInputConfirm = () => {
  const mid = route.params.id
  
  if (myTags.value.has(tagName)) {
    // 如果已经高亮，再次点击则取消
    request.delete(`/tag/remove/${mid}?tag=${encodeURIComponent(tagName)}`).then(() => {
      ElMessage.warning('标签已取消')
      fetchTagsData(mid) // 刷新标签云，标签会从红色变回灰色
    })
  } else {
    // 如果是灰色，点击则添加
    submitTagAction(tagName)
  }
}


// --- 路由与生命周期 ---

const goBack = () => router.back()
const goToDetail = (mid) => router.push(`/movie/${mid}`)

// 监听路由变化 (点击相似电影时复用组件)
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      window.scrollTo({ top: 0, behavior: 'smooth' })
      fetchDetailData(newId)
    }
  }
)

onMounted(() => {
  const mid = route.params.id
  fetchDetailData(route.params.id)
  // 核心：加载详情的同时，静默通知后端记录此次浏览
  request.post(`/user/history/add/${mid}`).catch(() => {
  })
})
</script>

<style scoped>
.detail-container {
  min-height: 100vh;
  background-color: #141414;
  color: white;
  padding-bottom: 50px;
}

.header {
  background-color: #141414; 
  
  /* 2. 增加毛玻璃效果 */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  
  /* 3. 确保挡住底下的内容 */
  z-index: 1000; 
  position: sticky;
  top: 0;
  
  padding: 15px 40px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: white;
}
:deep(.el-page-header__content) { color: white; }
.header-title { font-size: 18px; font-weight: bold; }

.content-area {
  padding: 0 40px;
}

/* 电影头部信息区 */
.movie-hero {
  display: flex;
  gap: 50px;
  margin-top: 20px;
  margin-bottom: 60px;
}

.poster-box {
  flex-shrink: 0;
  width: 320px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
}
.poster {
  width: 100%;
  display: block;
  aspect-ratio: 2 / 3;
  object-fit: cover;
}

.info-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.movie-title {
  font-size: 42px;
  font-weight: 900;
  margin: 0 0 15px 0;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
}

.meta-data {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.meta-tag { font-weight: bold; }

.genres { margin-bottom: 25px; }

.detail-item {
  margin-bottom: 12px;
  font-size: 15px;
  line-height: 1.5;
}
.detail-item .label { color: #888; }
.detail-item .value { color: #e5e5e5; }

.overview {
  margin-top: 20px;
  margin-bottom: 30px;
}
.overview h3 {
  font-size: 18px;
  color: #ccc;
  margin-bottom: 10px;
}
.overview p {
  font-size: 16px;
  color: #e5e5e5;
  line-height: 1.6;
  max-width: 800px;
}

.play-btn {
  font-weight: bold;
  border: none;
}
.play-btn:hover { background-color: #f40612; }

/* 相似电影推荐区 */
.similar-section {
  border-top: 1px solid #333;
  padding-top: 30px;
}
.section-title {
  font-size: 22px;
  font-weight: bold;
  color: #e5e5e5;
  margin-bottom: 20px;
}

/* 复用首页的横向滚动轨道样式 */
.horizontal-scroll-wrapper {
  display: flex;
  overflow-x: auto;
  gap: 15px;
  padding-bottom: 10px;
  scroll-behavior: smooth;
  scroll-snap-type: x mandatory;
  align-items: flex-start;
}
.horizontal-scroll-wrapper::-webkit-scrollbar { display: none; }

.movie-card-horizontal {
  flex: 0 0 160px; 
  width: 160px;
  max-width: 160px;
  cursor: pointer;
  position: relative;
  scroll-snap-align: start;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 240px;
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
.movie-card-horizontal:hover .image { transform: scale(1.05); }

.overlay-info {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background-color: rgba(0,0,0,0.8);
  border: 1px solid #ffb400;
  padding: 2px 6px;
  border-radius: 4px;
}
.score {
  color: #ffb400;
  font-weight: bold;
  font-size: 12px;
}

.movie-name-container {
  padding-top: 8px;
  width: 100%;
  overflow: hidden;
}
.movie-name {
  font-size: 14px;
  color: #e5e5e5;
  font-weight: bold;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 交互区样式 */
.interactive-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed #444;
}

.rating-box {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.rating-box .label {
  font-size: 16px;
  color: #ccc;
  margin-right: 15px;
}

/* 覆盖 el-rate 的星星大小 */
:deep(.el-rate__icon) {
  font-size: 24px;
}



.custom-tag {
  font-size: 14px;
  padding: 0 15px;
  height: 32px;
  line-height: 30px;
  background-color: #333;
  border-color: #444;
  color: #fff;
}

.new-tag-input {
  width: 120px;
  margin-left: 5px;
}

.button-new-tag {
  margin-left: 5px;
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
  background-color: transparent;
  color: #999;
  border: 1px dashed #666;
}

.button-new-tag:hover {
  color: #fff;
  border-color: #fff;
}

.interactive-tag {
  cursor: pointer;
  margin-right: 8px;
  margin-bottom: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 灰色标签 */
.el-tag--info.is-plain {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: #444;
  color: #999;
}

/* 高亮标签 */
.el-tag--danger.is-dark {
  background-color: #E50914; /* 经典红色高亮 */
  border-color: #E50914;
  box-shadow: 0 0 8px rgba(229, 9, 20, 0.4);
}

.interactive-tag:hover {
  transform: translateY(-2px);
  filter: brightness(1.2);
}

/* 标签云整体布局 */
.tag-cloud {
  display: flex;
  align-items: flex-start;
  margin-top: 15px;
}

.tag-cloud .label {
  font-size: 16px;
  color: #ccc;
  margin-right: 15px;
  flex-shrink: 0;
  padding-top: 4px;
}

.tags-wrapper {
  display: flex;
  flex-wrap: wrap; /* 自动换行 */
  gap: 8px;       /* 标签间距 */
  align-items: center;
}

.interactive-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.interactive-tag:hover {
  transform: scale(1.1);
  filter: brightness(1.2);
}

/* 调整输入框在暗色背景下的外观 */
:deep(.el-input-group__append) {
  background-color: #333;
  border-color: #444;
  color: #eee;
}

/* 详情页 - 均分 & 匹配度样式 */
.detail-score-box {
  margin: 16px 0;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-score-box .label {
  color: #888;
  flex-shrink: 0;
}
.score-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.avg-score {
  color: #f5c518;
  font-weight: bold;
}
.match-rate {
  color: #46d369;
  font-weight: bold;
  text-shadow: 0 0 4px rgba(70, 211, 105, 0.3);
}
.divider {
  color: #666;
}

/* 详情页特有的评分区域 */
.detail-scores {
  display: flex;
  gap: 20px;
  padding-bottom: 5px;
}

.score-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.score-val {
  font-size: 20px;
  font-weight: 800;
}

.avg .score-val { color: #F5C518; }
.match .score-val { color: #46D369; }

.score-label {
  font-size: 11px;
  color: #888;
  margin-top: 2px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 按钮悬停效果 */
.el-button:hover {
  background-color: #E50914 !important;
  color: white !important;
  border-color: #E50914 !important;
}

/* 相似推荐区域头部：标题和按钮左右对齐 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}
/* 滚动按钮样式 */
.scroll-controls .el-button {
  background-color: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
}
.scroll-controls .el-button:hover {
  background-color: #fff !important;
  color: #000 !important;
}

/* 核心播放按钮样式  */
.main-play-btn {
  font-size: 16px;
  font-weight: bold;
  padding: 0 35px;
  height: 44px;
  border-radius: 6px;
  background-color: #E50914; /* 经典流媒体红 */
  border: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 15px rgba(229, 9, 20, 0.3);
}

.main-play-btn:hover {
  background-color: #f40612 !important;
  transform: scale(1.03); /* 鼠标悬浮时微微放大，增加点击欲望 */
  box-shadow: 0 6px 20px rgba(229, 9, 20, 0.5);
}

.main-play-btn:active {
  transform: scale(0.98); /* 点击时微缩反馈 */
}
</style>