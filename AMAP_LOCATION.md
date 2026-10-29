# 家人位置 · 高德 Key

逆地理需要高德开放平台「Web 服务」Key：

1. https://console.amap.com/ 创建应用 → 添加 Key（服务平台选 Web 服务）
2. 本地：在 `.env` / `.env.local` 或运行环境设置 `AMAP_WEB_KEY=你的Key`
   或在 `application-local.yml`：
   ```yaml
   amap:
     web-key: ${AMAP_WEB_KEY:}
   ```
3. Docker / 服务器：在 compose `environment` 增加 `AMAP_WEB_KEY: ${AMAP_WEB_KEY:-}`，并在 `/root/Love/.env` 写入（勿提交仓库）
4. 未配置 Key 时：上报与列表仍可用，卡片只显示经纬度；配置后上报会写入 place_name / street / address

SQL（已有库需执行一次）:
`src/main/resources/db/update_add_family_member_location_address.sql`