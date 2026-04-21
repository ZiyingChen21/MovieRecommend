# 🎬 分布式高并发电影推荐系统 (Movie Recommendation System)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.4.5-brightgreen.svg)
![Maven](https://img.shields.io/badge/Maven-3.6.3-brightgreen.svg)
![Spark](https://img.shields.io/badge/Spark-MLlib%20%7C%20Streaming-E25A1C.svg)
![Redis](https://img.shields.io/badge/Redis-Cache%20%7C%20ZSet-DC382D.svg)
![Kafka](https://img.shields.io/badge/Kafka-Message%20Queue-231F20.svg)
![MongoDB](https://img.shields.io/badge/MongoDB-NoSQL-47A248.svg)

## 📖 项目简介
本项目是一个具备高并发承载能力与秒级实时响应的**流批一体电影推荐系统**。
系统采用算力与服务物理隔离的微服务架构，后端基于 Spring Boot 构建高可用调度与数据检索中心，底层融合 Spark MLlib 与 Spark Streaming 搭建离线+实时双轨推荐引擎。项目从底层架构规避了重型计算对 JVM 的资源抢占，并构建了完善的多级缓存防御体系。

## ⚙️ 核心技术栈
* **后端服务：** Java 8 / Spring Boot / Maven
* **大数据计算：** Scala / Spark Core / Spark SQL / Spark MLlib / Spark Streaming
* **中间件与存储：** Redis / MongoDB / Kafka
* **部署与运维：** Docker / 跨进程 Shell/CMD 脚本调度

## 🚀 核心架构与工程亮点

### 1. 算力隔离与高可用分布式调度
* **痛点：** 传统单体架构将重型矩阵计算塞入 JVM，易导致 OOM 与核心 Web 服务假死。
* **架构演进：** 采用计算与服务物理隔离架构。后端通过 `ProcessBuilder` 跨进程安全拉起 Spark 离线计算节点（Fat Jar）。
* **容灾保障：** 引入 **Redis + Lua 分布式锁**机制，保障大计算量任务在集群部署下的绝对幂等性；基于 MongoDB 状态机与后台独立线程，实现服务宕机重启后的无感自动补跑机制。

### 2. 离线/实时双轨推荐引擎
* **离线召回 (ALS)：** 基于交替最小二乘法 (ALS) 构建协同过滤模型，实现基于交叉验证与 RMSE 指标的网格超参数调优及模型持久化。
* **实时更新 (Streaming)：** 搭建 Kafka + Spark Streaming 流式计算管道，在召回阶段融合离线特征并引入 Log 动态奖惩机制，实现推荐列表的秒级平滑演进。
* **冷启动调优：** 针对新物品/零交互数据，基于 TF-IDF 提取电影内容特征并计算余弦相似度，有效缓解数据稀疏性并提升推荐多样性。

### 3. 高并发存储与多级缓存体系
* **O(1) 状态检索：** 针对高频用户足迹写入，后端基于 Redis ZSet 数据结构与时间戳权重，构建定长滑动窗口（Top 20），实现用户特征读写 O(1) 复杂度。
* **原子操作优化：** 读写分离检索体系配合原子状态更新，将数据库往返开销降低约 **50%**，复杂查询延迟稳定在 **50ms** 以内。

## 📂 项目结构
```text
MovieRecommend/
├── movie-rec-backend/      # Spring Boot 后端服务层 (API、分布式调度、缓存防御)
│   ├── scripts             # 大数据任务触发脚本 (bat/sh)
│   └── jar                 # 大数据 jar 包
├── recommend/              # Spark 大数据计算层 (ALS模型、TF-IDF、Streaming流计算)
└── movie-front/            # Vue.js 前端展示层
```

## 🛠️ 快速启动 (Quick Start)

### 1. 环境准备
确保本地已安装并启动以下组件：
* JDK 1.8+ & Scala 2.11+
* Redis (默认端口: 6379)
* MongoDB (默认端口: 27017)
* Kafka & Zookeeper (实时推荐模块需要)
* Hadoop / Spark 运行环境配置完毕 (SPARK_HOME)

### 2. 初始化数据
将 `/recommend/DataLoader/src/main/resources` 目录下的电影与评分基础数据集导入 MongoDB。

(可选) 执行一次大数据的预处理脚本 DataLoader。

### 3. 编译与启动

#### 步骤 1：编译打包 Spark 计算层
进入 `recommend` 目录，使用 Maven Shade Plugin 打包大数据模块：
```bash
mvn clean package
# 生成的胖包 (Fat Jar) 会自动放入后端的 jars 目录下
```

#### 步骤 2：启动 Spring Boot 后端服务
运行 `movie-rec-backend` 中的 `RecommendApplication.java`。
系统启动时将自动检测并调度大数据的离线基础推荐任务。

#### 步骤 3：启动前端
```bash
cd movie-front
npm install
npm run serve
```

## 🗺️ 后续演进路线 (Roadmap)
- [ ] **大模型智能观影助手 (RAG 架构)：** 未来若将大语言模型 (LLM) 与向量数据库引入系统也是一个极其不错的选择。结合现有用户画像与离线特征，可构建基于检索增强生成 (RAG) 的对话式精准荐片助手。
- [ ] **V2.0 极限高可用升级：** 针对未来潜在的千万级瞬时突发流量，计划在现有 Redis 缓存基础上，进一步落地细粒度的布隆过滤器 (防穿透)、动态 TTL 抖动 (防雪崩) 与 Redis 互斥自旋锁 (防击穿) 机制。
- [ ] **系统可观测性与降级机制：** 未来考虑完善接口级别的监控统计，并引入限流与熔断降级策略，全方位保障后端服务在极端并发下的绝对稳定。

## 📸 效果展示

###**step 1： 用户登陆**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/c99f677e-a3cf-48e8-b2c1-17b27088e869" />
**step 2： 首页用户冷启动**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/3f931d7e-1d69-405c-8f82-f54feb06c4fe" />
**step 3： 选择标签**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/82319958-3dfb-4e78-900f-95b07aaf721c" />
**step 4.1： 首页个性推荐展示**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/db5b904d-3a8c-49c2-bf4d-c2abca819466" />
**step 4.2： 首页个性推荐展示 海报懒加载**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/45d9e8ad-6dd9-4b80-bfc7-fb0b3a3cc9fd" />
**step 4.3： 首页个性推荐展示 实时推荐 + 统计推荐**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/f51e273f-8c9e-4768-b400-08945942a069" />
**step 4.4： 首页个性推荐展示 基于用户标签选择的推荐**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/bf7d84a9-c0c3-4bf3-a5cd-43ab16360f69" />
**step 5： 电影详情及相似电影推荐**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/34086b06-ce18-433e-82c7-7e5a2c70e129" />
**step 6：点击播放 跳转哔哩哔哩 寻找预告片**
<img width="2554" height="1185" alt="image" src="https://github.com/user-attachments/assets/fa936bcb-c5e5-4202-96aa-17d5fb943c44" />

**step 7：返回首页显示最近观看记录**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/1e8c1817-5f7c-4d23-a537-a00e4ac53ec0" />
**step 8.1：用户评分 之后“猜您喜欢”进行实时推荐**
<img width="2560" height="1271" alt="image" src="https://github.com/user-attachments/assets/772d8ae3-a7bb-4513-970a-48f02abae86a" />
**step 8.2：用户评分 之后“猜您喜欢”推荐发生变化**
<img width="2541" height="1264" alt="image" src="https://github.com/user-attachments/assets/2e10b492-8b09-4ce3-9b06-c625631a2801" />
**step 9：电影库**
<img width="2560" height="1399" alt="image" src="https://github.com/user-attachments/assets/8fe0661f-a8c8-47be-9b74-082d3204185e" /> 


