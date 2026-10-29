# DeepSeek 一句话点单（加购）

用自然语言（如「来份清淡的蛋羹」）匹配菜品并**加入购物车**，不会直接下单；下单仍走原购物车确认流程。

## 接口

- `POST /api/ai/cart`  
  Body: `{ "text": "来份清淡的蛋羹" }`  
  需登录 Session（点单方 / 做餐方均可）  
  成功：`{ code:200, message, data:{ added:[...], candidates:[...], autoAdded:true|false } }`  
  未配置 Key / 超时 / 调用失败：明确 `code` + `message`，**不会假加购**

## 本地配置 Key（勿提交仓库）

项目已 gitignore：`.env.local`、`application-local.yml`。

**推荐：环境变量**

```bash
# Windows PowerShell（当前会话）
$env:DEEPSEEK_API_KEY = (Get-Content D:\Love\.env.local | Where-Object { $_ -like 'DEEPSEEK_API_KEY=*' }).Split('=',2)[1]

# 或直接导出后启动
mvn spring-boot:run
```

IDEA：Run Configuration → Environment variables → `DEEPSEEK_API_KEY=你的key`

可选：项目根目录 `application-local.yml`（已在 `.gitignore`，且**不在** `src/main/resources`，不会打进 jar）+ 启动参数 `--spring.config.additional-location=optional:file:./` 并带 `spring.profiles.active=local`。更简单仍是设环境变量。

`application.yml` 中仅有占位：

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  timeout-seconds: 30
```

## Docker / 服务器发版

本机只打 jar：`mvn -DskipTests package`，**不要**在本流程里 SSH 部署。用户自行 `发版.sh`（仓库里 `linux.sh` 同类脚本）。

在服务器 `/root/Love` 的 `docker-compose`（或 `love-app` 服务）`environment` 中增加：

```yaml
DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY:-}
# 或直接写（注意权限与备份，勿提交到公开仓库）:
# DEEPSEEK_API_KEY: <your-deepseek-key>
```

宿主机可先：

```bash
export DEEPSEEK_API_KEY='你的key'
# 再执行发版脚本 / docker compose up
```

本仓库 `docker-compose.yml` 的 `app` 服务已预留 `DEEPSEEK_API_KEY` 透传。

## 前端

点单页搜索框下方：「一句话点单」输入 + 按钮。匹配成功 toast 提示并刷新购物车角标；模糊时展示候选芯片供点选加购。

## 相关文件

- `DeepSeekService.java` / `AiCartController.java`
- `application.yml` / `application-docker.yml` / `docker-compose.yml`
- `templates/index.html` / `static/js/love.js` / `static/css/love.css`