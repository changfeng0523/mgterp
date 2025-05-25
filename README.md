# 蘑菇头ERP进销存管理系统

一个基于Vue 3 + Spring Boot的现代化企业资源规划(ERP)系统，专注于进销存管理，支持AI自然语言交互。

## 🌟 项目特色

- 🎯 **现代化技术栈**：Vue 3 + Spring Boot 3 + MySQL
- 🤖 **AI智能交互**：集成DeepSeek AI，支持自然语言操作
- 📱 **响应式设计**：基于Element Plus的现代化UI界面
- 🔐 **安全认证**：JWT身份验证和权限管理
- 📊 **数据可视化**：ECharts图表展示业务数据
- 🚀 **前后端分离**：独立部署，易于扩展

## 🏗️ 系统架构

```
mgterp/
├── mogutouERP-vue/          # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── api/             # API接口
│   │   ├── components/      # 组件库
│   │   ├── views/           # 页面视图
│   │   ├── router/          # 路由配置
│   │   └── utils/           # 工具函数
│   └── package.json
├── mogutouERP-springboot/   # 后端项目 (Spring Boot)
│   ├── src/main/java/
│   │   ├── controller/      # 控制器
│   │   ├── service/         # 业务逻辑
│   │   ├── entity/          # 数据实体
│   │   ├── repository/      # 数据访问
│   │   └── nli/             # AI自然语言接口
│   └── pom.xml
└── README.md
```

## 🚀 快速开始

### 环境要求

- **前端**：Node.js 16+ 
- **后端**：Java 21+, Maven 3.6+
- **数据库**：MySQL 8.0+

### 安装步骤

#### 1. 克隆项目
```bash
git clone <repository-url>
cd mgterp
```

#### 2. 数据库配置
```sql
-- 创建数据库
CREATE DATABASE mgterp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 3. 后端启动
```bash
cd mogutouERP-springboot

# 配置数据库连接
# 编辑 src/main/resources/application.properties
# spring.datasource.url=jdbc:mysql://localhost:3306/mgterp
# spring.datasource.username=your_username
# spring.datasource.password=your_password

# 启动后端服务
mvn spring-boot:run
```

#### 4. 前端启动
```bash
cd mogutouERP-vue

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

#### 5. 访问系统
- 前端地址：http://localhost:9876
- 后端API：http://localhost:8081

## 📋 核心功能

### 🏢 企业管理
- 公司信息管理
- 员工档案管理
- 部门组织架构

### 📦 商品管理
- 商品信息维护
- 商品分类管理
- 库存实时监控

### 📋 订单管理
- 采购订单管理
- 销售订单管理
- 订单状态跟踪
- 订单确认流程

### 📊 库存管理
- 库存查询统计
- 入库出库操作
- 库存预警提醒
- 库存盘点功能

### 💰 财务管理
- 财务记录管理
- 收支统计分析
- 月度年度报表
- 利润分析图表

### 🤖 AI智能助手
- 自然语言订单创建
- 智能操作建议
- 语音指令支持
- 敏感操作确认

### 👥 用户管理
- 用户注册登录
- 角色权限管理
- 操作日志记录

## 🛠️ 技术栈

### 前端技术
- **框架**：Vue 3.5.13
- **构建工具**：Vite 6.2.0
- **UI组件**：Element Plus 2.9.7
- **状态管理**：Pinia 3.0.2
- **路由管理**：Vue Router 4.2.5
- **HTTP客户端**：Axios 1.6.2
- **图表库**：ECharts 5.6.0
- **样式预处理**：Sass 1.86.3

### 后端技术
- **框架**：Spring Boot 3.4.3
- **数据库**：MySQL 8.0
- **ORM框架**：Spring Data JPA + MyBatis Plus
- **安全认证**：JWT + Spring Security
- **工具库**：Hutool 5.8.26, Lombok 1.18.32
- **AI集成**：DeepSeek API
- **HTTP客户端**：OkHttp 4.12.0

## 🎯 AI功能说明

系统集成了DeepSeek AI，支持自然语言操作：

### 支持的自然语言命令

```
# 创建订单
"从京东采购100个螺丝刀"
"向苹果公司订购50台iPhone"

# 删除订单  
"删除订单号123的订单"
"取消ID为456的采购单"
```

### 使用方式
1. 在系统界面找到AI助手控制台
2. 输入自然语言指令
3. 系统自动解析并执行操作
4. 敏感操作会要求二次确认

## 📱 界面预览

系统采用现代化的响应式设计，支持：
- 深色/浅色主题切换
- 移动端适配
- 多语言支持
- 个性化设置

## 🔧 开发指南

### 项目结构说明

#### 前端目录结构
```
src/
├── api/                 # API接口定义
├── components/          # 公共组件
├── views/              # 页面组件
├── router/             # 路由配置
├── stores/             # Pinia状态管理
├── utils/              # 工具函数
├── styles/             # 全局样式
└── icons/              # SVG图标
```

#### 后端目录结构
```
src/main/java/com/mogutou/erp/
├── controller/         # REST控制器
├── service/           # 业务逻辑层
├── entity/            # JPA实体类
├── repository/        # 数据访问层
├── config/            # 配置类
├── common/            # 公共类
├── nli/               # AI自然语言接口
└── utils/             # 工具类
```

### 开发规范

1. **代码风格**：遵循阿里巴巴Java开发手册
2. **提交规范**：使用Conventional Commits
3. **分支管理**：采用Git Flow工作流
4. **测试覆盖**：单元测试覆盖率>80%

## 🚀 部署指南

### Docker部署（推荐）

```bash
# 构建前端
cd mogutouERP-vue
npm run build

# 构建后端
cd ../mogutouERP-springboot
mvn clean package

# 使用Docker Compose部署
docker-compose up -d
```

### 传统部署

1. **前端部署**：构建静态文件部署到Nginx
2. **后端部署**：打包JAR文件部署到服务器
3. **数据库**：配置MySQL主从复制

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

## 🙏 致谢

- [Vue.js](https://vuejs.org/) - 渐进式JavaScript框架
- [Spring Boot](https://spring.io/projects/spring-boot) - Java应用开发框架
- [Element Plus](https://element-plus.org/) - Vue 3组件库
- [ECharts](https://echarts.apache.org/) - 数据可视化图表库
- [DeepSeek](https://www.deepseek.com/) - AI大语言模型

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交Issue：[GitHub Issues](../../issues)
- 邮箱：your-email@example.com

---

⭐ 如果这个项目对您有帮助，请给我们一个Star！
