# 🚀 浪漫点单系统 - 快速启动指南

## 第一步：准备数据库

### 1. 确保MySQL已安装并运行
```bash
# 检查MySQL状态
mysql --version
```

### 2. 创建数据库和表
```bash
# 方式一：使用命令行
mysql -u root -p < src/main/resources/db/schema.sql

# 方式二：登录MySQL后执行
mysql -u root -p
source F:/Project/Excel/src/main/resources/db/schema.sql
```

### 3. 验证数据
```sql
USE love_order_system;

-- 查看分类
SELECT * FROM category;

-- 查看菜品
SELECT * FROM dish;

-- 查看用户
SELECT * FROM user;
```

## 第二步：配置应用

### 1. 修改数据库配置
编辑 `src/main/resources/application.yml`，修改数据库用户名和密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/love_order_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root        # 改成你的MySQL用户名
    password: root        # 改成你的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 2. （可选）修改端口
如果8080端口被占用，可以修改：
```yaml
server:
  port: 8081  # 改成其他端口
```

## 第三步：运行应用

### 方式一：使用Maven（推荐）
```bash
# 在项目根目录执行
cd F:\Project\Excel

# 编译并运行
mvn spring-boot:run
```

### 方式二：打包后运行
```bash
# 编译打包
mvn clean package

# 运行JAR包
java -jar target/excel-copy-tool-1.0.0.jar
```

### 方式三：使用IDE
1. 用IDEA打开项目
2. 找到 `ExcelApplication.java`
3. 右键 -> Run

## 第四步：访问应用

### 前端页面（给她用）
打开浏览器访问：
```
http://localhost:8080
```

你会看到粉色浪漫的"公主的御膳房"页面！

### API测试
可以使用Postman或curl测试API：

```bash
# 获取所有菜品
curl http://localhost:8080/api/dish/list

# 获取分类
curl http://localhost:8080/api/category/list

# 获取推荐菜品
curl http://localhost:8080/api/dish/recommended

# 随机惊喜
curl http://localhost:8080/api/dish/surprise

# 创建订单
curl -X POST http://localhost:8080/api/order/create \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {
        "dishId": 1,
        "quantity": 2,
        "remark": "少盐"
      }
    ],
    "remark": "尽快送达"
  }'
```

## 第五步：测试功能

### 基础功能测试清单

#### ✅ 菜品展示
- [ ] 能看到6个分类按钮（中餐、西餐、甜品等）
- [ ] 点击分类能筛选菜品
- [ ] 菜品卡片显示名称、描述、价格
- [ ] 标签正确显示（拿手菜、新品等）

#### ✅ 购物车
- [ ] 点击"加入购物车"成功
- [ ] 右下角购物车图标显示数量
- [ ] 点击购物车能看到清单
- [ ] 总价计算正确

#### ✅ 下单
- [ ] 确认下单成功
- [ ] 看到成功提示"下单成功！马上为你准备~ 💕"
- [ ] 购物车清空

#### ✅ 惊喜功能
- [ ] 点击左下角"随机惊喜"按钮
- [ ] 弹出随机菜品推荐
- [ ] 显示暖心话语

## 常见问题

### ❌ 问题1：启动失败，提示数据库连接错误
**解决方案：**
1. 检查MySQL是否启动
2. 确认用户名密码正确
3. 确认数据库 `love_order_system` 已创建

### ❌ 问题2：端口8080被占用
**解决方案：**
修改 `application.yml` 中的端口号为其他值，如8081

### ❌ 问题3：页面空白或样式不对
**解决方案：**
1. 清除浏览器缓存
2. 检查浏览器控制台是否有错误
3. 确认Thymeleaf依赖已添加

### ❌ 问题4：菜品图片不显示
**说明：**
当前版本使用emoji代替图片，这是正常的设计。后续可以添加真实图片上传功能。

## 个性化定制

### 修改欢迎语
编辑 `src/main/resources/templates/index.html`，找到：
```html
<h1>👑 公主的御膳房 👑</h1>
<p>为你精心准备每一道美味 💕</p>
```
改成你想要的文字。

### 修改主题颜色
在数据库中执行：
```sql
-- 改成紫色主题
UPDATE user SET theme_color = '#DDA0DD' WHERE id = 1;

-- 改成莫兰迪色
UPDATE user SET theme_color = '#E8D5C4' WHERE id = 1;
```

### 添加新菜品
```sql
INSERT INTO dish (category_id, name, description, price_type, price, is_signature, is_recommended) 
VALUES (1, '可乐鸡翅', '甜香入味，小朋友最爱', '亲亲', 2, 1, 1);
```

### 设置她的生日
```sql
-- 假设她的生日是3月15日
UPDATE user SET birthday = '2000-03-15' WHERE id = 1;
```

## 下一步优化建议

1. **添加真实图片**
   - 实现图片上传功能
   - 为每道菜拍摄精美照片

2. **完善后台管理**
   - 创建管理员界面
   - 可视化操作菜品和订单

3. **集成微信通知**
   - 使用Server酱推送消息
   - 她下单后你立即收到微信通知

4. **添加成就系统**
   - 实现完整的成就解锁逻辑
   - 添加成就展示页面

5. **数据统计**
   - 添加图表展示点单统计
   - 分析她的口味偏好

## 💝 温馨提示

- 记得定期备份数据库
- 可以根据实际情况调整菜品和价格
- 多观察她的使用反馈，持续优化
- 最重要的是：用心做好每一道菜！

---

**祝你和她甜蜜幸福！💕**

如有问题，欢迎随时咨询~
