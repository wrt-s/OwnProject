# Love — H5 小程序风格点单系统

基于原项目 `D:\Excel`（Spring Boot 恋爱点单）复制改造的 **移动端 H5** 应用。  
外观接近小程序（底部 Tab、全屏手机布局、柔和玫瑰主题），**不是微信小程序**，无需微信开发者工具。在 IntelliJ IDEA 中像普通 Spring Boot 项目一样运行即可。

## 技术栈

- Spring Boot 3.2 + Thymeleaf + MyBatis Plus
- MySQL（与 Excel 共用同一套库配置）
- 前端：原生 HTML / CSS / JS（`static/css/love.css`、`static/js/love.js`）
- 登录：手机号 + MD5 密码，HttpSession；情侣双角色 `ORDERER` / `CHEF`

## 在 IDEA 中打开并运行

1. 打开 IntelliJ IDEA → **File → Open** → 选择目录 `D:\Love`
2. 等待 Maven 导入完成
3. 如尚未迁移角色，执行一次：`src/main/resources/db/update_couple_roles.sql`
4. 主类：`com.wrt.excel.LoveApplication` → Run  
   或：`mvn spring-boot:run` / `start.bat`
5. 浏览器：**http://localhost:8081** → 先登录

## 演示账号（密码均为 123456）

| 角色 | 说明 | 手机号 | 密码 |
|------|------|--------|------|
| **ORDERER 点单方** | 浏览、下单、玩法、看自己的订单 | `17743761205` | `123456` |
| **CHEF 做餐方** | 厨房接单改状态、菜品后台 `/admin`、看板 | `18233008281` | `123456` |

库中密码存 MD5：`e10adc3949ba59abbe56e057f20f883e`。

## 端口

| 项目 | 端口 |
|------|------|
| Excel（原项目） | 8080 |
| **Love** | **8081** |

## 信息架构（H5）

主路径：**登录 → 浏览菜品 → 购物车 → 下单 → 追踪订单**。

| Tab | 功能 |
|-----|------|
| **点单** | 搜索、分类、菜品卡、惊喜条、加购；购物车 FAB + 抽屉 |
| **订单** | 点单方：我的订单/取消；做餐方：厨房列表 + 状态流转 |
| **玩法** | 惊喜点单 + 签到/抽奖/押注/喂食/成就/收藏 |
| **我的** | 昵称 + 角色徽章、生活入口；做餐方可见后台/厨房；退出登录 |

未登录访问首页会跳转 `/login`。Admin 仅做餐方可进。

## 鉴权 API

- `POST /api/auth/login` `{ phone, password }` → Session + 用户资料
- `POST /api/auth/logout`
- `GET /api/auth/me`
- 拦截器：菜品/分类只读公开；下单等需登录；`/api/order/status`、`/api/order/list`、菜品写、`/admin` 需 **CHEF**

## 核心业务 API（沿用）

响应：`{ code: 200, message, data }`

- 菜品 / 分类 / 惊喜 / 下单 / 取消 / 币余额 等与原 Excel 一致

## 项目结构要点

```
templates/index.html     # H5 主壳
templates/login.html     # 登录页
static/css/love.css      # 视觉（已重写）
static/js/love.js        # 点单/购物车/订单/鉴权
AuthController / AuthService / AuthInterceptor
db/update_couple_roles.sql
```

## 数据库

`application.yml`：`127.0.0.1:3306/love_order_system`，`root` / `123456`。  
角色迁移脚本会把 id=1 设为点单方、id=2 设为做餐方。

## 注意事项

- 不是微信小程序。
- 改前端后请 Ctrl+F5 强刷；改 Java 后重启 `LoveApplication`。
- 前端不再写死 `userId=1`，以登录 Session 为准。

## H5 UI（2026-10-24 统一改版）

- 设计：软玫瑰强调色、白卡片、约 430px 手机壳、共享顶栏/返回、底部安全区、中文文案
- 共享资源：`static/css/love.css`、`static/js/love.js`（首页）、`static/js/love-page.js`（子页鉴权/返回/toast）
- 子页均走登录会话用户（`/api/auth/me`），不再写死 `userId=1`
- 做餐方专属：`/admin`、`/dashboard`（ORDERER 会被重定向）
- 玩法入口含：签到、抽奖、押注、喂食、成就、收藏、**盲盒** `/blindbox`
- 本地预览：启动 Spring Boot 后打开 `http://localhost:8081/` ，演示账号见登录页

```bash
# Windows
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd spring-boot:run
# 或双击 start.bat
```

## 安全与完善（2026-10-24）

- **服务端 Session userId**：下单、取消、签到、抽奖、收藏、盲盒、押注、喂食等自助 API 强制使用登录 Session，忽略客户端伪造的 `userId`；CHEF 在厨房/管理接口可代查目标用户。加币 `/api/coin/add` 仅做餐方。
- **盲盒历史**：表 `blind_box_record` + `GET /api/blindbox/history`，前端 `/blindbox` 展示服务端记录。执行 `db/update_add_blind_box_record.sql`。
- **管理后台 H5**：`/admin` 支持菜品图片上传（`/api/dish/uploadImage`）与操作日志列表/清空（`/api/operation-log/*`）。

## AI 一句话点单

自然语言加购（仅购物车，不下单）。详见 [DEPLOY_AI.md](./DEPLOY_AI.md)。  
本地设置环境变量 `DEEPSEEK_API_KEY`（可用 gitignore 的 `.env.local`）；服务器在 compose environment 中配置后发版。
