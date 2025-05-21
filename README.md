# 进销存管理系统 (MoguTou ERP)

## 项目概述

进销存管理系统（MoguTou ERP）是一个全面的企业资源规划系统，专为中小型企业设计，用于管理日常业务运营的各个方面，包括库存管理、订单处理、财务统计、员工管理和公司信息管理等。系统采用前后端分离架构，提供直观的用户界面和强大的后端支持，帮助企业提高运营效率，降低管理成本。

## 功能特点

### 1. 库存管理
- 商品信息维护（名称、编码、分类、规格等）
- 库存数量实时监控
- 库存预警提醒
- 库存入库/出库记录管理
- 库存位置管理

### 2. 订单管理
- 销售订单处理
- 采购订单管理
- 订单状态跟踪（待处理、处理中、已完成、已取消）
- 订单详情查看
- 客户信息管理

### 3. 财务管理
- 收入和支出记录
- 财务数据统计和分析
- 利润计算
- 财务报表生成
- 数据可视化展示

### 4. 员工管理
- 员工信息维护
- 部门管理
- 职位管理
- 员工状态跟踪（在职、离职等）

### 5. 公司管理
- 公司基本信息维护
- 多公司支持
- 公司与员工关联管理

### 6. 用户管理
- 用户认证与授权
- 角色权限控制
- 安全登录

## 技术栈

### 前端
- **框架**: Vue 3
- **状态管理**: Pinia
- **UI组件库**: Element Plus
- **路由**: Vue Router
- **HTTP客户端**: Axios
- **图表库**: ECharts
- **构建工具**: Vite

### 后端
- **框架**: Spring Boot 3.4.3
- **ORM**: Spring Data JPA
- **安全框架**: Spring Security
- **数据库**: MySQL 8.0
- **API文档**: Swagger
- **构建工具**: Maven

## 系统架构

```
进销存管理系统
├── 前端 (Vue 3)
│   ├── 用户界面
│   ├── 状态管理
│   ├── API调用
│   └── 数据可视化
└── 后端 (Spring Boot)
    ├── RESTful API
    ├── 业务逻辑
    ├── 数据访问
    └── 安全认证
```

## 安装指南

### 前提条件
- Node.js 16+
- Java 21
- MySQL 8.0+
- Maven 3.8+

### 后端部署
1. 克隆仓库
   ```bash
   git clone https://github.com/yourusername/mogutou-erp.git
   cd mogutou-erp
   ```

2. 配置数据库
   - 创建MySQL数据库
   ```sql
   CREATE DATABASE mgterp CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
   - 修改`application.properties`或`application.yml`中的数据库连接配置

3. 构建并启动后端
   ```bash
   cd mogutouERP-springboot
   mvn clean install
   mvn spring-boot:run
   ```
   后端服务将在 http://localhost:8081 上运行

4. 数据库配置文件在mogutouERP-springboot\sql\init.sql

### 前端部署
1. 安装依赖
   ```bash
   cd mogutouERP-vue
   npm install
   ```

2. 配置环境变量
   - 创建或修改`.env.local`文件，设置API基础URL
   ```
   VITE_BASE_API=http://localhost:8081
   ```

3. 启动开发服务器
   ```bash
   npm run dev
   ```
   前端将在 http://localhost:9876 上运行

4. 构建生产版本
   ```bash
   npm run build
   ```

## 使用指南

### 初始登录
- 默认管理员账号: `admin`
- 默认密码: `admin123`

### 主要功能操作流程

#### 库存管理
1. 导航至"库存管理"
2. 可添加、编辑、删除库存商品
3. 查看库存详情和库存历史

#### 订单管理
1. 导航至"订单管理"
2. 选择"销售订单"或"采购订单"
3. 创建新订单，填写订单信息
4. 管理订单状态和查看订单详情

#### 财务管理
1. 导航至"财务管理"
2. 添加收入/支出记录
3. 查看财务统计图表
4. 按日期范围筛选财务数据

#### 员工管理
1. 导航至"公司管理" > "员工管理"
2. 添加、编辑员工信息
3. 管理员工部门和职位

#### 公司管理
1. 导航至"公司管理"
2. 添加、编辑公司信息
3. 关联公司与员工

## 数据库结构

系统包含以下主要数据表：

- `users`: 用户账户信息
- `goods`: 商品基本信息
- `inventory`: 库存信息
- `orders`: 订单主表
- `order_goods`: 订单商品关联表
- `company`: 公司信息
- `staff`: 员工信息
- `finance_record`: 财务记录

## 开发指南

### 目录结构

#### 后端
```
mogutouERP-springboot/
├── src/main/java/com/mogutou/erp/
│   ├── common/       # 通用工具类和常量
│   ├── config/       # 配置类
│   ├── controller/   # 控制器
│   ├── entity/       # 实体类
│   ├── repository/   # 数据访问层
│   ├── service/      # 业务逻辑层
│   └── MogutouErpApplication.java  # 应用入口
└── src/main/resources/
    ├── application.yml  # 应用配置
    └── db/migration/    # 数据库迁移脚本
```

#### 前端
```
mogutouERP-vue/
├── public/         # 静态资源
├── src/
│   ├── api/        # API调用
│   ├── assets/     # 资源文件
│   ├── components/ # 组件
│   ├── icons/      # 图标
│   ├── router/     # 路由配置
│   ├── store/      # 状态管理
│   ├── styles/     # 样式文件
│   ├── utils/      # 工具函数
│   ├── views/      # 页面视图
│   ├── App.vue     # 根组件
│   └── main.js     # 入口文件
└── vite.config.js  # Vite配置
```

### API文档

启动后端服务后，可通过以下URL访问API文档：
- Swagger UI: http://localhost:8081/swagger-ui.html

## 贡献指南

1. Fork 项目仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件

## 联系方式

项目维护者 - 哈振宇

---

© 2024 MoguTou ERP. 保留所有权利。 