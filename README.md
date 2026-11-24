# 智游 AItravel 后端

基于 Spring Boot + MyBatis-Plus 开发的旅游推荐与 AI 行程规划平台后端，提供用户鉴权、POI 检索、收藏、行程生成、行程管理及后台管理接口。

## 技术栈

- Java 8
- Spring Boot 2.6
- MyBatis-Plus
- MySQL 8.0
- Redis
- JWT
- Spring Security
- 高德地图 API
- 大模型接口（SSE 流式返回）

## 功能模块

### 用户模块

- 手机号密码登录
- 用户注册
- 图形验证码生成与 Redis 缓存校验
- JWT 访问令牌与刷新令牌
- 用户资料查看、昵称/头像/密码修改
- 退出登录，令牌失效

### 旅游内容模块

- 景点、美食、游玩项目 POI 查询
- 城市、类型、关键词检索
- 高德地图关键字检索接入
- 首页轮播图查询与管理

### 收藏模块

- POI 收藏保存
- 取消收藏
- 个人收藏列表查询
- 收藏状态与数量统计

### AI 行程模块

- 旅游意图解析
- 逐日行程结构化生成
- SSE 流式返回生成进度
- 行程保存、列表查询、详情查询
- 行程节点编辑与行程微调

### 后台管理模块

- 用户列表查询
- 无效账号删除
- 轮播图新增、编辑、删除
- 轮播图启用状态管理

## 仓库地址

```text
https://github.com/wanglirong-sketch/aitravel-server
```

前端仓库：

```text
https://github.com/wanglirong-sketch/aitravel-web
```

## 目录结构

```text
src/main/java/com/example/aitravelserver
├── common       # 通用配置、JWT、异常处理、返回体
├── controller   # 接口控制器
├── dto          # 请求参数对象
├── entity       # 数据库实体
├── mapper       # 数据访问层
├── service      # 业务接口与实现
└── vo           # 响应视图对象
```

## 本地启动

### 1. 环境准备

- JDK 8
- Maven 3.6+
- MySQL 8.0
- Redis

### 2. 创建数据库

```sql
CREATE DATABASE travel DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

导入项目 SQL 文件后，在 `application.yml` 中配置数据库连接。

### 3. 修改配置

配置文件：

```text
src/main/resources/application.yml
```

需要核对：

- MySQL 地址、端口、用户名、密码
- Redis 地址、端口
- JWT 密钥
- 高德地图 Key
- 大模型 API Key

### 4. 启动项目

运行启动类：

```text
AItravelServerApplication
```

后端默认地址：

```text
http://localhost:8080
```

## 统一返回格式

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

状态码说明：

| code | 含义        |
| ---- | --------- |
| 0    | 请求成功      |
| 400  | 参数或业务校验失败 |
| 401  | 未登录或登录过期  |
| 403  | 无权限       |
| 500  | 系统异常      |

## 测试账号

```text
管理员手机号：13900000001
管理员密码：abc123456
```
