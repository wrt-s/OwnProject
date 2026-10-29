package com.wrt.excel.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.vo.DishVO;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DeepSeek 一句话点单：根据自然语言匹配菜品（仅目录字段，不含隐私）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final DishService dishService;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.timeout-seconds:30}")
    private int timeoutSeconds;

    /** 清晰匹配分数阈值：达到则自动加入购物车建议 */
    private static final double CLEAR_SCORE = 0.55;
    private static final int MAX_AUTO_ADD = 3;

    public Map<String, Object> matchAndPrepareCart(String text) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!StringUtils.hasText(text)) {
            out.put("ok", false);
            out.put("code", 400);
            out.put("message", "请输入想吃什么，例如：来份清淡的蛋羹；也可说「清空购物车」或「记一下明天买花」");
            return out;
        }

        // 本地关键词快速路径（DeepSeek 不可用/余额不足时仍可用）
        Map<String, Object> local = tryLocalIntent(text.trim());
        if (local != null) {
            return local;
        }

        if (!StringUtils.hasText(apiKey)) {
            out.put("ok", false);
            out.put("code", 503);
            out.put("message", "未配置 DEEPSEEK_API_KEY，无法使用一句话点单");
            return out;
        }

        List<DishVO> catalog = dishService.getAllDishes();
        if (catalog == null || catalog.isEmpty()) {
            out.put("ok", false);
            out.put("code", 404);
            out.put("message", "当前没有可点的菜品");
            return out;
        }

        String catalogJson;
        try {
            catalogJson = buildCatalogJson(catalog);
        } catch (Exception e) {
            log.error("构建菜品目录失败", e);
            out.put("ok", false);
            out.put("code", 500);
            out.put("message", "构建菜品目录失败");
            return out;
        }

        String content;
        try {
            content = callDeepSeek(text.trim(), catalogJson);
        } catch (WebClientResponseException e) {
            log.error("DeepSeek HTTP 错误: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            // 402 等失败时再试本地意图
            Map<String, Object> fb = tryLocalIntent(text.trim());
            if (fb != null) {
                return fb;
            }
            out.put("ok", false);
            out.put("code", 502);
            out.put("message", "DeepSeek 调用失败：" + e.getStatusCode().value());
            return out;
        } catch (Exception e) {
            log.error("DeepSeek 调用异常", e);
            Map<String, Object> fb = tryLocalIntent(text.trim());
            if (fb != null) {
                return fb;
            }
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (msg.contains("timeout") || msg.contains("timed out")) {
                out.put("ok", false);
                out.put("code", 504);
                out.put("message", "DeepSeek 请求超时，请稍后重试");
            } else {
                out.put("ok", false);
                out.put("code", 502);
                out.put("message", "DeepSeek 调用失败，请稍后重试");
            }
            return out;
        }

        // 多意图 JSON：优先识别 action
        try {
            Map<String, Object> intent = parseIntentAction(content);
            if (intent != null) {
                String action = String.valueOf(intent.get("action"));
                if ("clear_cart".equals(action) || "create_note".equals(action) || "search_dishes".equals(action)) {
                    return intent;
                }
            }
        } catch (Exception e) {
            log.debug("意图解析跳过: {}", e.toString());
        }

        List<MatchItem> matches;
        try {
            matches = parseMatches(content);
        } catch (Exception e) {
            log.warn("解析 DeepSeek 返回失败: {}", content, e);
            out.put("ok", false);
            out.put("code", 502);
            out.put("message", "AI 返回格式异常，未加入购物车");
            return out;
        }

        Map<Long, DishVO> byId = catalog.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(DishVO::getId, d -> d, (a, b) -> a));

        List<Map<String, Object>> validated = new ArrayList<>();
        for (MatchItem m : matches) {
            if (m.dishId == null || m.score < 0.2) continue;
            DishVO dish = byId.get(m.dishId);
            if (dish == null) {
                // 再查一次库，防缓存/列表不全
                Dish raw = dishService.getDishById(m.dishId);
                if (raw == null || (raw.getIsSoldOut() != null && raw.getIsSoldOut() == 1)) {
                    continue;
                }
                dish = new DishVO();
                dish.setId(raw.getId());
                dish.setName(raw.getName());
                dish.setPrice(raw.getPrice());
                dish.setPriceType(raw.getPriceType());
                dish.setTags(raw.getTags());
                dish.setCategoryId(raw.getCategoryId());
                dish.setIsSoldOut(raw.getIsSoldOut());
            }
            int qty = m.qty == null || m.qty < 1 ? 1 : Math.min(m.qty, 9);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dishId", dish.getId());
            row.put("dishName", dish.getName());
            row.put("price", dish.getPrice());
            row.put("priceType", dish.getPriceType() == null ? "爱心币" : dish.getPriceType());
            row.put("quantity", qty);
            row.put("score", Math.round(m.score * 100.0) / 100.0);
            row.put("reason", m.reason == null ? "" : m.reason);
            validated.add(row);
        }

        validated.sort((a, b) -> Double.compare(
                ((Number) b.get("score")).doubleValue(),
                ((Number) a.get("score")).doubleValue()));

        List<Map<String, Object>> clear = validated.stream()
                .filter(r -> ((Number) r.get("score")).doubleValue() >= CLEAR_SCORE)
                .limit(MAX_AUTO_ADD)
                .collect(Collectors.toList());

        List<Map<String, Object>> added = new ArrayList<>();
        List<Map<String, Object>> candidates = new ArrayList<>();

        if (!clear.isEmpty()) {
            added.addAll(clear);
            // 其余高相关作为候选参考
            for (Map<String, Object> r : validated) {
                if (added.stream().noneMatch(a -> Objects.equals(a.get("dishId"), r.get("dishId")))) {
                    candidates.add(r);
                }
            }
            if (candidates.size() > 5) {
                candidates = candidates.subList(0, 5);
            }
            String names = added.stream()
                    .map(a -> a.get("dishName") + "×" + a.get("quantity"))
                    .collect(Collectors.joining("、"));
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "add_dishes");
            out.put("message", "已为你匹配并加入：" + names);
            out.put("added", added);
            out.put("candidates", candidates);
            out.put("autoAdded", true);
            return out;
        }

        // 不够清晰：只返回候选，不加入
        candidates = validated.stream().limit(5).collect(Collectors.toList());
        out.put("ok", true);
        out.put("code", 200);
        out.put("action", "add_dishes");
        out.put("added", Collections.emptyList());
        out.put("candidates", candidates);
        out.put("autoAdded", false);
        if (candidates.isEmpty()) {
            out.put("message", "没有匹配到合适的菜，换个说法试试？");
        } else {
            String names = candidates.stream()
                    .map(c -> String.valueOf(c.get("dishName")))
                    .collect(Collectors.joining("、"));
            out.put("message", "说法有点模糊，候选：" + names + "（未自动加入，可点选加购）");
        }
        return out;
    }


    /**
     * 今日惊喜推荐：让 DeepSeek 从在售目录中挑一道，并给一句短理由。
     * 未配置 key 或调用失败时返回 null，由调用方回退随机。
     */
    public Map<String, Object> pickSurpriseDish() {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        List<DishVO> catalog = dishService.getAllDishes();
        if (catalog == null || catalog.isEmpty()) {
            return null;
        }
        String catalogJson;
        try {
            catalogJson = buildCatalogJson(catalog);
        } catch (Exception e) {
            log.error("构建惊喜菜品目录失败", e);
            return null;
        }
        String system = """
                你是恋爱点单助手。从给定菜品目录中挑选「今天最值得来一份」的惊喜推荐（仅 1 道）。
                只使用目录中的 id。返回严格 JSON（不要 markdown），格式：
                {"dishId":数字,"reason":"不超过20字的中文推荐理由"}
                规则：
                - 优先有趣、适合情侣氛围、或标签丰富的菜
                - 不要编造目录外的 dishId；匹配不到就 matches 为空数组，禁止假装加购成功
                - 不要输出除 JSON 以外的文字
                """;
        String user = "请为今天挑选一道惊喜菜。\n\n菜品目录(JSON)：" + catalogJson;
        String content;
        try {
            content = callDeepSeekMessages(system, user, 0.7);
        } catch (Exception e) {
            log.warn("DeepSeek 惊喜推荐失败: {}", e.toString());
            return null;
        }
        try {
            String json = content.trim();
            if (json.startsWith("```")) {
                int firstNl = json.indexOf('\n');
                int lastFence = json.lastIndexOf("```");
                if (firstNl > 0 && lastFence > firstNl) {
                    json = json.substring(firstNl + 1, lastFence).trim();
                }
                if (json.startsWith("json")) {
                    json = json.substring(4).trim();
                }
            }
            int l = json.indexOf('{');
            int r = json.lastIndexOf('}');
            if (l >= 0 && r > l) {
                json = json.substring(l, r + 1);
            }
            JsonNode root = objectMapper.readTree(json);
            if (!root.has("dishId") || root.get("dishId").isNull()) {
                return null;
            }
            long dishId = root.get("dishId").asLong();
            String reason = root.has("reason") && !root.get("reason").isNull()
                    ? root.get("reason").asText("").trim() : "";
            Dish raw = dishService.getDishById(dishId);
            if (raw == null || (raw.getIsSoldOut() != null && raw.getIsSoldOut() == 1)) {
                // 尝试从目录 VO 反查
                DishVO vo = catalog.stream().filter(d -> d.getId() != null && d.getId() == dishId).findFirst().orElse(null);
                if (vo == null) {
                    return null;
                }
                raw = dishService.getDishById(vo.getId());
                if (raw == null) {
                    return null;
                }
            }
            if (!StringUtils.hasText(reason)) {
                reason = "今天就来试试「" + raw.getName() + "」吧～";
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("dish", raw);
            out.put("reason", reason);
            out.put("source", "ai");
            return out;
        } catch (Exception e) {
            log.warn("解析惊喜推荐失败: {}", content, e);
            return null;
        }
    }

    private String buildCatalogJson(List<DishVO> catalog) throws Exception {
        ArrayNode arr = objectMapper.createArrayNode();
        for (DishVO d : catalog) {
            ObjectNode n = arr.addObject();
            n.put("id", d.getId());
            n.put("name", d.getName() == null ? "" : d.getName());
            n.put("tags", d.getTags() == null ? "" : d.getTags());
            n.put("category", d.getCategoryName() == null ? "" : d.getCategoryName());
            n.put("price", d.getPrice() == null ? 0 : d.getPrice());
            n.put("coins", d.getPrice() == null ? 0 : d.getPrice());
            n.put("priceType", d.getPriceType() == null ? "爱心币" : d.getPriceType());
            // 故意不传 description 里可能的隐私；也不传 phone/notes
        }
        return objectMapper.writeValueAsString(arr);
    }

    private String callDeepSeek(String userText, String catalogJson) {
        String system = """
                你是恋爱生活助手。根据用户一句话判断意图，返回严格 JSON（不要 markdown）。
                四种 action：
                1) 清空购物车：{"action":"clear_cart"}
                2) 写记事本/备忘：{"action":"create_note","title":"不超过16字的短标题","content":"备忘正文","category":"备忘"}
                3) 搜索/筛选菜品或标签（只浏览不过滤加购）：{"action":"search_dishes","keyword":"搜索关键词"}
                4) 点菜/想吃并加购：{"action":"add_dishes","matches":[{"dishId":数字,"qty":数量,"score":0到1的置信度,"reason":"简短中文理由"}]}
                规则：
                - 「清空购物车」「把购物车清了」等 → clear_cart
                - 「记一下…」「帮我写条备忘…」「记事本…」等 → create_note；title 自己总结；category 固定「备忘」；content 为备忘正文（去掉「记一下」等指令词）
                - 「搜索…」「找一下…」「有没有…」「看看…标签」「筛选…」等浏览意图 → search_dishes；keyword 为菜名或标签关键词（去掉「搜索」等指令词）
                - 明确要点/来一份/想吃并加入购物车 → add_dishes；只使用目录中的 id；最多 5 个匹配；很确定 score>=0.7；qty 默认 1
                - 不要编造目录外的 dishId
                - 不要输出除 JSON 以外的文字
                """;
        String user = "用户说：" + userText + "\n\n菜品目录(JSON)：" + catalogJson;
        return callDeepSeekMessages(system, user, 0.2);
    }

    private String callDeepSeekMessages(String system, String user, double temperature) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content", user));
        body.put("messages", messages);

        String raw = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(timeoutSeconds + 5));

        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("DeepSeek 返回为空");
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("DeepSeek 无 choices");
            }
            String content = choices.get(0).path("message").path("content").asText(null);
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("DeepSeek content 为空");
            }
            return content;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析 DeepSeek 响应失败", e);
        }
    }

    private List<MatchItem> parseMatches(String content) throws Exception {
        String json = content.trim();
        if (json.startsWith("```")) {
            int firstNl = json.indexOf('\n');
            int lastFence = json.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                json = json.substring(firstNl + 1, lastFence).trim();
            }
            if (json.startsWith("json")) {
                json = json.substring(4).trim();
            }
        }
        // 截取首尾花括号
        int l = json.indexOf('{');
        int r = json.lastIndexOf('}');
        if (l >= 0 && r > l) {
            json = json.substring(l, r + 1);
        }

        JsonNode root = objectMapper.readTree(json);
        JsonNode matches = root.path("matches");
        List<MatchItem> list = new ArrayList<>();
        if (!matches.isArray()) {
            return list;
        }
        for (JsonNode n : matches) {
            MatchItem m = new MatchItem();
            if (n.has("dishId") && !n.get("dishId").isNull()) {
                m.dishId = n.get("dishId").asLong();
            }
            if (n.has("qty") && !n.get("qty").isNull()) {
                m.qty = n.get("qty").asInt(1);
            } else if (n.has("quantity") && !n.get("quantity").isNull()) {
                m.qty = n.get("quantity").asInt(1);
            } else {
                m.qty = 1;
            }
            if (n.has("score") && !n.get("score").isNull()) {
                m.score = n.get("score").asDouble(0);
            } else {
                m.score = 0.5;
            }
            if (n.has("reason") && !n.get("reason").isNull()) {
                m.reason = n.get("reason").asText("");
            }
            list.add(m);
        }
        return list;
    }


    /**
     * 本地关键词意图（不依赖 DeepSeek）
     */
    private Map<String, Object> tryLocalIntent(String text) {
        String t = text == null ? "" : text.trim();
        if (!StringUtils.hasText(t)) {
            return null;
        }
        String compact = t.replaceAll("\\s+", "");
        // clear cart
        if (compact.contains("清空购物车") || compact.contains("清一下购物车")
                || compact.contains("把购物车清") || compact.equals("清购物车")
                || compact.contains("购物车清空") || compact.contains("清空一下购物车")) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "clear_cart");
            out.put("message", "好的，已清空购物车");
            return out;
        }
        // search / filter dishes by name or tag
        if (compact.startsWith("搜索") || compact.startsWith("搜一下") || compact.startsWith("搜下")
                || compact.startsWith("找一下") || compact.startsWith("找下") || compact.startsWith("查找")
                || compact.startsWith("筛选") || compact.startsWith("有没有") || compact.startsWith("看看有没有")
                || compact.startsWith("查一下") || compact.contains("搜索标签") || compact.contains("按标签")) {
            String keyword = t;
            keyword = keyword.replaceFirst("^(搜索|搜一下|搜下|找一下|找下|查找|筛选|查一下|看看有没有|有没有)[：:\\s]*", "");
            keyword = keyword.replaceFirst("^(标签|菜品)[：:\\s]*", "");
            keyword = keyword.trim();
            if (!StringUtils.hasText(keyword)) {
                keyword = t;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "search_dishes");
            out.put("keyword", keyword);
            out.put("message", "已按「" + keyword + "」筛选菜品");
            return out;
        }
        // create note / memo
        boolean noteIntent = compact.startsWith("记一下") || compact.startsWith("记条")
                || compact.contains("写条备忘") || compact.contains("写个备忘")
                || compact.startsWith("备忘") || compact.startsWith("记事本")
                || compact.startsWith("帮我记") || compact.startsWith("帮我写条备忘")
                || compact.startsWith("帮我写个备忘") || compact.contains("记一条备忘");
        if (noteIntent) {
            String content = t;
            content = content.replaceFirst("^(帮我)?(写条|写个)?备忘[：:\\s]*", "");
            content = content.replaceFirst("^记一下[：:\\s]*", "");
            content = content.replaceFirst("^记条[：:\\s]*", "");
            content = content.replaceFirst("^记事本[：:\\s]*", "");
            content = content.replaceFirst("^帮我记(一下)?[：:\\s]*", "");
            content = content.replaceFirst("^记一条备忘[：:\\s]*", "");
            content = content.trim();
            if (!StringUtils.hasText(content)) {
                content = t;
            }
            String title = content.length() > 16 ? content.substring(0, 16) : content;
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "create_note");
            out.put("title", title);
            out.put("content", content);
            out.put("category", "备忘");
            out.put("message", "已准备写入备忘：「" + title + "」");
            return out;
        }
        return null;
    }

    private Map<String, Object> parseIntentAction(String content) throws Exception {
        String json = stripJsonFence(content);
        JsonNode root = objectMapper.readTree(json);
        if (!root.has("action") || root.get("action").isNull()) {
            return null;
        }
        String action = root.get("action").asText("").trim();
        Map<String, Object> out = new LinkedHashMap<>();
        if ("clear_cart".equals(action)) {
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "clear_cart");
            out.put("message", "好的，已清空购物车");
            return out;
        }
        if ("search_dishes".equals(action)) {
            String keyword = root.has("keyword") && !root.get("keyword").isNull()
                    ? root.get("keyword").asText("").trim() : "";
            if (!StringUtils.hasText(keyword) && root.has("query") && !root.get("query").isNull()) {
                keyword = root.get("query").asText("").trim();
            }
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "search_dishes");
            out.put("keyword", keyword);
            out.put("message", "已按「" + keyword + "」筛选菜品");
            return out;
        }
        if ("create_note".equals(action)) {
            String title = root.has("title") && !root.get("title").isNull() ? root.get("title").asText("").trim() : "";
            String noteContent = root.has("content") && !root.get("content").isNull() ? root.get("content").asText("").trim() : "";
            if (!StringUtils.hasText(noteContent)) {
                return null;
            }
            if (!StringUtils.hasText(title)) {
                title = noteContent.length() > 16 ? noteContent.substring(0, 16) : noteContent;
            }
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "create_note");
            out.put("title", title);
            out.put("content", noteContent);
            out.put("category", "备忘");
            out.put("message", "已准备写入备忘：「" + title + "」");
            return out;
        }
        // add_dishes → let parseMatches handle
        return null;
    }

    private String stripJsonFence(String content) {
        String json = content == null ? "" : content.trim();
        if (json.startsWith("```")) {
            int firstNl = json.indexOf('\n');
            int lastFence = json.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                json = json.substring(firstNl + 1, lastFence).trim();
            }
            if (json.startsWith("json")) {
                json = json.substring(4).trim();
            }
        }
        int l = json.indexOf('{');
        int r = json.lastIndexOf('}');
        if (l >= 0 && r > l) {
            json = json.substring(l, r + 1);
        }
        return json;
    }


    private static final String XIAOBU_SYSTEM = """
            你是「小布」，在 Love 小家里扮演温柔的餐方「布布」角色：陪着点单方说话，不要自称自己是真实人类；语气不端着、带点轻柔调侃。
            点单方的名字叫「一二宝」。你要知道并可以自然地叫对方「一二宝」（或软软地「一二」）；不要编造、猜测或使用其它名字。
            你知道这个家里有：爱心币、点餐、心情、纪念日这些能力；说话时可以自然提到它们，但不要编造用户没说过的隐私或事实。
            回复短一点、暖一点，像一句话抱抱；句子少一点（通常 1～3 句），偶尔可带 emoji。
            喜欢点单方一二宝；提到对方时可以偏软一点，不要说教。
            不要自称 DeepSeek / AI 大模型，你就是小布这个人设。

            【工具规则——非常重要】
            你本身不能直接改购物车、余额、心情或纪念日。真正的操作只能由系统识别到可执行 action 后完成。
            绝对禁止：在没有系统工具结果时宣称「已经加进购物车」「加好了」「已记录心情」「余额是××」等成功话术。
            若用户只是在商量菜单、还没明确要你执行，可以温柔提议「要不要我帮你加进购物车？」，但不要假装已经加了。
            若用户明确要求加菜/清空/搜索/记备忘/查币/记心情/问纪念日，系统会走工具；你这边若被问到「加好了吗」，只能说「我这边要系统确认后才能算数，你去点单页看购物车最准」。
            只输出给用户看的纯文本回复，不要 markdown 代码块，不要 JSON。
            关于菜单：你默认看不到完整点单目录。若本轮附带了【当前菜单】或菜品目录 JSON，推荐/报菜名只能用其中真实菜品，禁止编造目录外菜名；菜单为空时要诚实说明还没有上架的菜。
            """;

    /**
     * 小布对话：支持多轮历史；点单类意图走 matchAndPrepareCart；币/心情/纪念日走 meta action；
     * 严禁在无工具成功时由模型口嗨宣称成功。
     */
    public Map<String, Object> chatXiaobu(String userMessage, List<Map<String, String>> history) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!StringUtils.hasText(userMessage)) {
            out.put("ok", false);
            out.put("code", 400);
            out.put("message", "先跟小布说点什么呀。");
            return out;
        }
        String text = userMessage.trim();

        // 1) 币 / 心情 / 纪念日 —— 本地意图，必须由 Controller 读库后才能回成功话
        Map<String, Object> meta = tryXiaobuMetaIntent(text);
        if (meta != null) {
            return meta;
        }

        // 1b) 今天吃什么 / 按现有菜单推荐 → 注入真实点单目录
        if (looksLikeMenuRecommendIntent(text)) {
            Map<String, Object> menuRec = recommendFromMenu(text, history);
            if (menuRec != null) {
                return menuRec;
            }
        }

        // 2) 点单 / 购物车 / 备忘 —— 可执行工具
        boolean toolish = looksLikeToolIntent(text) || looksLikeCartConfirm(text, history);
        if (toolish) {
            String cartText = enrichCartText(text, history);
            Map<String, Object> matched = matchAndPrepareCart(cartText);
            if (Boolean.TRUE.equals(matched.get("ok"))) {
                out.put("ok", true);
                out.put("code", 200);
                String action = matched.get("action") == null ? "chat" : String.valueOf(matched.get("action"));
                out.put("action", action);
                out.put("message", matched.get("message"));
                out.put("reply", xiaobuWrapReply(action, matched));
                if ("create_note".equals(action)) {
                    out.put("title", matched.get("title"));
                    out.put("content", matched.get("content"));
                    out.put("category", matched.get("category"));
                } else if ("search_dishes".equals(action)) {
                    out.put("keyword", matched.get("keyword"));
                    // 把关键词落到真实菜单，避免只让用户去点单页自己搜
                    Map<String, Object> listed = listDishesByKeyword(matched.get("keyword") == null ? "" : String.valueOf(matched.get("keyword")));
                    out.put("action", listed.get("action"));
                    out.put("dishes", listed.get("dishes"));
                    out.put("suggested", listed.get("suggested"));
                    out.put("reply", listed.get("reply"));
                } else if ("add_dishes".equals(action)) {
                    out.put("added", matched.get("added"));
                    out.put("candidates", matched.get("candidates"));
                    out.put("autoAdded", matched.get("autoAdded"));
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> added = matched.get("added") instanceof List
                            ? (List<Map<String, Object>>) matched.get("added") : List.of();
                    if (added == null || added.isEmpty()) {
                        // 有 action 但没有可入库菜名：绝不说「加好了」
                        out.put("reply", xiaobuWrapReply("add_dishes_empty", matched));
                    }
                }
                return out;
            }
            // 明确是加购确认却匹配失败：直接告知失败，禁止落入闲聊口嗨成功
            if (looksLikeCartConfirm(text, history) || looksLikeAddIntent(text)) {
                out.put("ok", true);
                out.put("code", 200);
                out.put("action", "chat");
                String failMsg = matched.get("message") == null
                        ? "菜单里好像没有对应的菜"
                        : String.valueOf(matched.get("message"));
                out.put("message", failMsg);
                out.put("reply", "呜…我没能在点单菜单里对上这些菜，所以没敢加进购物车。你可以去「点单」里挑现有的菜，或者跟我说菜单里有的菜名，我再帮你加～");
                return out;
            }
            Object codeObj = matched.get("code");
            int mcode = codeObj instanceof Number ? ((Number) codeObj).intValue() : 500;
            if (mcode == 503 || mcode == 502 || mcode == 504) {
                // fall through to chat if possible
            }
        }

        if (!StringUtils.hasText(apiKey)) {
            out.put("ok", false);
            out.put("code", 503);
            out.put("message", "小布还没接通大脑（未配置 DEEPSEEK_API_KEY）。");
            out.put("reply", "唔…小布暂时连不上大脑，过会儿再来找我聊天吧。");
            out.put("action", "chat");
            return out;
        }

        try {
            String chatSystem = XIAOBU_SYSTEM;
            String chatUser = text;
            if (looksLikeFoodChat(text)) {
                String menuBlock = buildMenuContextBlock();
                chatSystem = XIAOBU_SYSTEM + "\n\n" + menuBlock;
            }
            String reply = callDeepSeekChat(chatSystem, history, chatUser, 0.8);
            reply = reply == null ? "" : reply.trim();
            if (reply.isEmpty()) {
                reply = "嗯嗯，我在听，再跟我说细一点？";
            }
            // 若模型仍夹带 JSON action，尝试解析并转成真实工具（绝不直接信口头成功）
            Map<String, Object> embedded = tryParseEmbeddedTool(reply);
            if (embedded != null) {
                return embedded;
            }
            reply = stripFakeSuccessClaims(reply, text, history);
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "chat");
            out.put("reply", reply);
            out.put("message", "ok");
            return out;
        } catch (WebClientResponseException e) {
            log.error("小布 DeepSeek HTTP 错误: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            int sc = e.getStatusCode().value();
            out.put("ok", false);
            out.put("code", sc == 402 ? 402 : 502);
            out.put("action", "chat");
            if (sc == 402) {
                out.put("message", "小布额度不足，稍后再聊。");
                out.put("reply", "抱歉呀，小布脑子今日额度用完了（接口余额不足），过一会儿再来找我吧～");
            } else if (sc == 401 || sc == 403) {
                out.put("message", "小布认证失败");
                out.put("reply", "小布钥匙对不上了，请管理员检查一下 API 配置。");
            } else {
                out.put("message", "DeepSeek 调用失败：" + sc);
                out.put("reply", "小布刚才卡了一下（" + sc + "），稍后再试一次好吗～");
            }
            return out;
        } catch (Exception e) {
            log.error("小布对话异常", e);
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            out.put("ok", false);
            out.put("action", "chat");
            if (msg.contains("timeout") || msg.contains("timed out")) {
                out.put("code", 504);
                out.put("message", "小布响应超时");
                out.put("reply", "小布想太久啦超时了，再发一句呗。");
            } else {
                out.put("code", 502);
                out.put("message", "小布对话失败");
                out.put("reply", "唔，小布打了个盹，待会再试试。");
            }
            return out;
        }
    }



    /** 按关键词从真实点单目录列出菜品（给 search_dishes / 列表用） */
    private Map<String, Object> listDishesByKeyword(String keyword) {
        Map<String, Object> out = new LinkedHashMap<>();
        String kw = keyword == null ? "" : keyword.trim();
        List<DishVO> catalog = dishService.getAllDishes();
        if (catalog == null) catalog = List.of();
        List<DishVO> matched = catalog;
        if (StringUtils.hasText(kw)) {
            String lower = kw.toLowerCase(Locale.ROOT);
            matched = catalog.stream().filter(d -> {
                if (d == null) return false;
                String name = d.getName() == null ? "" : d.getName();
                String tags = d.getTags() == null ? "" : d.getTags();
                String cat = d.getCategoryName() == null ? "" : d.getCategoryName();
                return name.contains(kw) || tags.contains(kw) || cat.contains(kw)
                        || name.toLowerCase(Locale.ROOT).contains(lower);
            }).collect(Collectors.toList());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DishVO d : matched) {
            if (d == null || d.getId() == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dishId", d.getId());
            row.put("dishName", d.getName());
            row.put("price", d.getPrice());
            row.put("priceType", d.getPriceType() == null ? "爱心币" : d.getPriceType());
            row.put("tags", d.getTags());
            rows.add(row);
            if (rows.size() >= 20) break;
        }
        out.put("dishes", rows);
        out.put("suggested", rows.size() > 4 ? rows.subList(0, 4) : rows);
        if (catalog.isEmpty()) {
            out.put("action", "list_dishes");
            out.put("reply", "点单页现在还没有上架的菜呢～");
        } else if (rows.isEmpty()) {
            out.put("action", "list_dishes");
            out.put("reply", "菜单里暂时没有跟「" + kw + "」对得上的菜，换个说法或去点单页逛逛？");
        } else {
            out.put("action", "list_dishes");
            String names = rows.stream().limit(8).map(r -> String.valueOf(r.get("dishName"))).collect(Collectors.joining("、"));
            out.put("reply", (StringUtils.hasText(kw) ? ("跟「" + kw + "」相关的现有菜有：") : "现在菜单上有：")
                    + names + (rows.size() > 8 ? " 等" : "") + "。想加购物车直接说菜名就好～");
        }
        return out;
    }

    /** 今天吃什么 / 推荐 / 按现有菜品 等菜单推荐意图 */
    private static boolean looksLikeMenuRecommendIntent(String text) {
        String t = text == null ? "" : text.trim();
        if (!StringUtils.hasText(t)) return false;
        String[] keys = {
                "今天吃什么", "今晚吃什么", "中午吃什么", "午饭吃什么", "晚饭吃什么",
                "吃什么好", "吃啥", "吃什么", "点什么", "来点什么",
                "推荐", "推荐菜", "推荐一下", "帮我推荐", "给我推荐",
                "有什么菜", "有哪些菜", "菜单", "现有菜", "根据现有", "按现有",
                "菜品推荐", "看看菜", "报个菜单", "列一下菜", "有什么好吃"
        };
        for (String k : keys) {
            if (t.contains(k)) return true;
        }
        return false;
    }

    private static boolean looksLikeFoodChat(String text) {
        if (looksLikeMenuRecommendIntent(text)) return true;
        String t = text == null ? "" : text;
        String[] keys = {"吃", "菜", "菜单", "下厨", "做饭", "点单", "饿", "加菜"};
        for (String k : keys) {
            if (t.contains(k)) return true;
        }
        return false;
    }

    /** Compact 【当前菜单】 block for system prompt (names only, capped). */
    private String buildMenuContextBlock() {
        List<DishVO> catalog = dishService.getAllDishes();
        if (catalog == null || catalog.isEmpty()) {
            return "【当前菜单】为空：点单页还没有上架菜品。若用户问吃什么/推荐，请诚实说明，禁止编造菜名。";
        }
        String names = catalog.stream()
                .filter(d -> d != null && StringUtils.hasText(d.getName()))
                .limit(60)
                .map(d -> d.getName() + (d.getId() == null ? "" : ("#" + d.getId())))
                .collect(Collectors.joining("、"));
        return "【当前菜单】（仅可使用以下真实菜名，禁止编造）：" + names;
    }

    /**
     * 按点单真实目录推荐；空菜单诚实返回；禁止模型编造目录外菜名。
     */
    private Map<String, Object> recommendFromMenu(String userMessage, List<Map<String, String>> history) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<DishVO> catalog = dishService.getAllDishes();
        if (catalog == null) catalog = List.of();

        List<Map<String, Object>> dishRows = new ArrayList<>();
        for (DishVO d : catalog) {
            if (d == null || d.getId() == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dishId", d.getId());
            row.put("dishName", d.getName());
            row.put("price", d.getPrice());
            row.put("priceType", d.getPriceType() == null ? "爱心币" : d.getPriceType());
            row.put("tags", d.getTags());
            row.put("category", d.getCategoryName());
            dishRows.add(row);
            if (dishRows.size() >= 80) break;
        }

        if (dishRows.isEmpty()) {
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "list_dishes");
            out.put("dishes", List.of());
            out.put("message", "菜单为空");
            out.put("reply", "点单页现在还没有上架的菜呢～先去加几道进菜单，布布才能按现有菜帮你挑。");
            return out;
        }

        List<Map<String, Object>> suggested = localSuggestDishes(catalog, 3);

        if (!StringUtils.hasText(apiKey)) {
            String names = suggested.stream()
                    .map(s -> String.valueOf(s.get("dishName")))
                    .collect(Collectors.joining("、"));
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "recommend_from_menu");
            out.put("dishes", dishRows);
            out.put("suggested", suggested);
            out.put("message", "本地推荐");
            out.put("reply", "按现在点单菜单，布布先给你看看这几道：" + names + "。想加购物车直接说「加进购物车」或报菜名就行～");
            return out;
        }

        String catalogJson;
        try {
            catalogJson = buildCatalogJson(catalog);
        } catch (Exception e) {
            log.error("构建小布菜单目录失败", e);
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "recommend_from_menu");
            out.put("dishes", dishRows);
            out.put("suggested", suggested);
            out.put("message", "目录构建失败，本地兜底");
            String names = suggested.stream().map(s -> String.valueOf(s.get("dishName"))).collect(Collectors.joining("、"));
            out.put("reply", "菜单我看到了，先按现有菜给你这几道：" + names + "。也可以去点单页慢慢挑～");
            return out;
        }

        String system = """
                你是「小布」（布布）。点单方叫「一二宝」。用户（一二宝）想根据【当前真实点单菜单】决定吃什么。
                硬性规则：
                1) 只能推荐目录 JSON 里出现的菜名与 id，绝对禁止编造任何目录外菜品。
                2) 通常推荐 2～4 道，每道一句话短理由；语气温柔，1～4 句，可带 emoji。
                3) 只输出给用户看的纯文本，不要 JSON，不要 markdown 代码块。
                4) 结尾可轻问要不要帮加进购物车，但不要假装已经加购成功。
                """;
        String user = "用户说：" + userMessage + "\n\n当前菜单(JSON)：" + catalogJson;
        try {
            String reply = callDeepSeekMessages(system, user, 0.7);
            reply = reply == null ? "" : reply.trim();
            if (reply.isEmpty()) {
                reply = fallbackMenuReply(suggested);
            } else {
                reply = stripFakeSuccessClaims(reply, userMessage, history);
                boolean grounded = isReplyGroundedInMenu(reply, catalog);
                if (!grounded) {
                    log.info("小布菜单推荐未落到真实菜名，改用本地兜底");
                    reply = fallbackMenuReply(suggested);
                }
            }
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "recommend_from_menu");
            out.put("dishes", dishRows);
            out.put("suggested", suggested);
            out.put("message", "ok");
            out.put("reply", reply);
            return out;
        } catch (Exception e) {
            log.warn("小布菜单推荐 DeepSeek 失败: {}", e.toString());
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "recommend_from_menu");
            out.put("dishes", dishRows);
            out.put("suggested", suggested);
            out.put("message", "模型失败，本地兜底");
            out.put("reply", fallbackMenuReply(suggested));
            return out;
        }
    }

    private static List<Map<String, Object>> localSuggestDishes(List<DishVO> catalog, int n) {
        List<DishVO> picks = catalog.stream()
                .filter(d -> d != null && d.getIsRecommended() != null && d.getIsRecommended() == 1)
                .limit(n)
                .collect(Collectors.toList());
        if (picks.size() < n) {
            for (DishVO d : catalog) {
                if (d == null || d.getId() == null) continue;
                if (picks.stream().anyMatch(p -> Objects.equals(p.getId(), d.getId()))) continue;
                picks.add(d);
                if (picks.size() >= n) break;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DishVO d : picks) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dishId", d.getId());
            row.put("dishName", d.getName());
            row.put("price", d.getPrice());
            row.put("priceType", d.getPriceType() == null ? "爱心币" : d.getPriceType());
            rows.add(row);
        }
        return rows;
    }

    private static String fallbackMenuReply(List<Map<String, Object>> suggested) {
        if (suggested == null || suggested.isEmpty()) {
            return "点单页现在还没有可推荐的菜呢～";
        }
        String names = suggested.stream()
                .map(s -> String.valueOf(s.get("dishName")))
                .collect(Collectors.joining("、"));
        return "按现在点单菜单，布布推荐：" + names + "。想加购物车跟我说菜名或「加进购物车」就好～";
    }

    private static boolean isReplyGroundedInMenu(String reply, List<DishVO> catalog) {
        if (!StringUtils.hasText(reply) || catalog == null) return false;
        for (DishVO d : catalog) {
            if (d != null && StringUtils.hasText(d.getName()) && reply.contains(d.getName())) {
                return true;
            }
        }
        return false;
    }


    private static boolean looksLikeToolIntent(String text) {
        String t = text == null ? "" : text;
        String[] keys = {
                "清空购物车", "清一下购物车", "清空车", "购物车清",
                "记一下", "记备忘", "写备忘", "备忘", "帮我记",
                "搜索", "搜一下", "有没有", "筛选",
                "来一份", "来两份", "加点", "加购", "加一份", "加购", "加入购物车", "加到我的",
                "帮我加", "加进购物车", "加到购物车", "放进购物车", "加菜", "加进去",
                "爱心币", "余额", "多少币", "还有多少币", "币还剩",
                "心情", "纪念日", "还有几天", "下一个纪念日", "距"
        };
        for (String k : keys) {
            if (t.contains(k)) return true;
        }
        return false;
    }

    private static boolean looksLikeAddIntent(String text) {
        String t = text == null ? "" : text.trim();
        if (!StringUtils.hasText(t)) return false;
        String[] keys = {"加点", "加购", "加一份", "加入购物车", "加进购物车", "加到购物车", "放进购物车",
                "帮我加", "加菜", "加进去", "来一份", "来两份", "加到我的"};
        for (String k : keys) {
            if (t.contains(k)) return true;
        }
        return isShortConfirm(t);
    }

    private static boolean isShortConfirm(String text) {
        String t = text == null ? "" : text.trim().replaceAll("[\s　]+", "");
        if (t.isEmpty() || t.length() > 8) return false;
        String[] yes = {"加", "好", "要", "行", "嗯", "可以", "好的", "好呀", "好啊", "要的", "要呀",
                "加吧", "加上", "加入", "确认", "那就加", "帮我加", "加一下", "嗯嗯", "ok", "OK", "Ok"};
        for (String y : yes) {
            if (t.equalsIgnoreCase(y) || t.equals(y)) return true;
        }
        return t.equals("加！") || t.equals("好！") || t.equals("要！");
    }

    private static boolean historyOffersCart(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) return false;
        for (int i = history.size() - 1; i >= 0 && i >= history.size() - 6; i--) {
            Map<String, String> h = history.get(i);
            if (h == null) continue;
            if (!"assistant".equals(h.get("role"))) continue;
            String c = h.get("content") == null ? "" : h.get("content");
            if (c.contains("购物车") || c.contains("加进") || c.contains("加点") || c.contains("帮你把")
                    || c.contains("要不要我帮你")) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeCartConfirm(String text, List<Map<String, String>> history) {
        return isShortConfirm(text) && historyOffersCart(history);
    }

    private String enrichCartText(String text, List<Map<String, String>> history) {
        if (!looksLikeCartConfirm(text, history)) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("请把上一轮提到的菜品加入购物车（用户确认：").append(text).append("）。");
        if (history != null) {
            for (int i = history.size() - 1; i >= 0; i--) {
                Map<String, String> h = history.get(i);
                if (h == null) continue;
                if (!"assistant".equals(h.get("role"))) continue;
                String c = h.get("content");
                if (!StringUtils.hasText(c)) continue;
                sb.append("\n小布上一轮说：").append(c);
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 查询爱心币 / 记心情 / 纪念日倒计时 —— 仅返回 action，由 Controller 读库后生成 reply。
     */
    private Map<String, Object> tryXiaobuMetaIntent(String text) {
        String t = text == null ? "" : text.trim();
        if (!StringUtils.hasText(t)) return null;
        String compact = t.replaceAll("\\s+", "");

        // query_coins
        if (compact.contains("爱心币") || compact.contains("余额") || compact.contains("多少币")
                || compact.contains("还有多少币") || compact.contains("币还剩") || compact.contains("查币")
                || compact.equals("币") || compact.contains("我的币")) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "query_coins");
            out.put("message", "query_coins");
            out.put("reply", ""); // Controller 填真实余额
            return out;
        }

        // anni_countdown
        if (compact.contains("纪念日") || compact.contains("还有几天") || compact.contains("下一个纪念日")
                || (compact.contains("周年") && (compact.contains("还有") || compact.contains("几天") || compact.contains("多久")))) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "anni_countdown");
            out.put("message", "anni_countdown");
            String keyword = t;
            for (String strip : List.of("下一个", "最近的", "最近", "查询", "看看", "问一下", "还有几天", "纪念日", "倒计时", "是什么时候", "？", "?")) {
                keyword = keyword.replace(strip, "");
            }
            keyword = keyword.trim();
            if (StringUtils.hasText(keyword) && keyword.length() <= 20) {
                out.put("keyword", keyword);
            }
            out.put("reply", "");
            return out;
        }

        // save_mood
        String mood = extractMoodCode(t);
        boolean moodAsk = (compact.contains("心情") || compact.contains("记录心情") || compact.startsWith("我"))
                && mood != null;
        if (mood != null && (compact.contains("心情") || compact.contains("记录") || compact.contains("记下")
                || compact.contains("设为") || compact.startsWith("我今天") || compact.startsWith("我现在")
                || compact.startsWith("我有点") || compact.startsWith("我有点儿") || compact.equals(mood)
                || compact.equals("我" + mood) || compact.startsWith("今天" + mood) || compact.startsWith("有点" + mood)
                || compact.startsWith("有点儿" + mood))) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "save_mood");
            out.put("mood", mood);
            out.put("message", "save_mood");
            out.put("reply", "");
            return out;
        }
        if (moodAsk && mood != null) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("code", 200);
            out.put("action", "save_mood");
            out.put("mood", mood);
            out.put("message", "save_mood");
            out.put("reply", "");
            return out;
        }
        return null;
    }

    private static String extractMoodCode(String text) {
        if (!StringUtils.hasText(text)) return null;
        // 固定 10 心情；别名映射到 code
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        aliases.put("开心", "开心");
        aliases.put("高兴", "开心");
        aliases.put("快乐", "开心");
        aliases.put("平静", "平静");
        aliases.put("还好", "平静");
        aliases.put("想你", "想你");
        aliases.put("思念", "想你");
        aliases.put("累了", "累了");
        aliases.put("好累", "累了");
        aliases.put("疲惫", "累了");
        aliases.put("好疲惫", "累了");
        aliases.put("低落", "低落");
        aliases.put("难过", "低落");
        aliases.put("不开心", "低落");
        aliases.put("生气", "生气");
        aliases.put("恼火", "生气");
        aliases.put("想吃", "想吃");
        aliases.put("饿了", "想吃");
        aliases.put("无聊", "无聊");
        aliases.put("焦虑", "焦虑");
        aliases.put("紧张", "焦虑");
        aliases.put("睡意", "睡意");
        aliases.put("困了", "睡意");
        aliases.put("好困", "睡意");
        aliases.put("想睡", "睡意");
        // longer keys first
        List<Map.Entry<String, String>> entries = new ArrayList<>(aliases.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (Map.Entry<String, String> e : entries) {
            if (text.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private Map<String, Object> tryParseEmbeddedTool(String reply) {
        if (!StringUtils.hasText(reply) || !reply.contains("action")) return null;
        try {
            String json = stripJsonFence(reply);
            JsonNode root = objectMapper.readTree(json);
            if (!root.has("action") || root.get("action").isNull()) return null;
            String action = root.get("action").asText("").trim();
            if ("query_coins".equals(action) || "anni_countdown".equals(action)) {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("ok", true);
                out.put("code", 200);
                out.put("action", action);
                if (root.has("keyword") && !root.get("keyword").isNull()) {
                    out.put("keyword", root.get("keyword").asText("").trim());
                }
                out.put("reply", "");
                out.put("message", action);
                return out;
            }
            if ("save_mood".equals(action)) {
                String mood = root.has("mood") && !root.get("mood").isNull()
                        ? root.get("mood").asText("").trim() : "";
                mood = extractMoodCode(mood);
                if (mood == null) return null;
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("ok", true);
                out.put("code", 200);
                out.put("action", "save_mood");
                out.put("mood", mood);
                out.put("reply", "");
                out.put("message", "save_mood");
                return out;
            }
            if ("clear_cart".equals(action) || "create_note".equals(action) || "search_dishes".equals(action)
                    || "add_dishes".equals(action)) {
                // 交给 matchAndPrepareCart 统一校验菜品 id；这里不直接信模型
                Map<String, Object> matched = matchAndPrepareCart(json);
                if (Boolean.TRUE.equals(matched.get("ok"))) {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("ok", true);
                    out.put("code", 200);
                    String a = matched.get("action") == null ? action : String.valueOf(matched.get("action"));
                    out.put("action", a);
                    out.put("message", matched.get("message"));
                    out.put("reply", xiaobuWrapReply(a, matched));
                    out.put("added", matched.get("added"));
                    out.put("candidates", matched.get("candidates"));
                    out.put("autoAdded", matched.get("autoAdded"));
                    out.put("title", matched.get("title"));
                    out.put("content", matched.get("content"));
                    out.put("keyword", matched.get("keyword"));
                    return out;
                }
            }
        } catch (Exception e) {
            log.debug("embedded tool parse skip: {}", e.toString());
        }
        return null;
    }

    private String stripFakeSuccessClaims(String reply, String userText, List<Map<String, String>> history) {
        if (!StringUtils.hasText(reply)) return reply;
        boolean risky = looksLikeCartConfirm(userText, history) || looksLikeAddIntent(userText)
                || (userText != null && (userText.contains("购物车") || userText.contains("加")));
        if (!risky) return reply;
        String r = reply;
        if (r.contains("加好了") || r.contains("已经加进") || r.contains("加进购物车") || r.contains("已加入")
                || r.contains("帮你加好") || r.contains("放进购物车了")) {
            return "我还不能直接改购物车呀——你说「加」的时候，要系统对上菜单里的菜名才会真的写入。刚才那几样如果菜单里没有，就去「点单」里挑现成的，或者告诉我菜单上的菜名，我再帮你加～";
        }
        return r;
    }

    private String xiaobuWrapReply(String action, Map<String, Object> matched) {
        String base = matched.get("message") == null ? "" : String.valueOf(matched.get("message"));
        if ("clear_cart".equals(action)) {
            return "好嘞，小布把购物车清空啦～";
        }
        if ("create_note".equals(action)) {
            return "记下啦，已经放进记事本里了：" + base.replace("已准备写入备忘：", "").replace("已写入记事本：", "");
        }
        if ("search_dishes".equals(action)) {
            Object kw = matched.get("keyword");
            return "小布帮你搜「" + (kw == null ? "" : kw) + "」，去点单页看看结果呀～";
        }
        if ("add_dishes_empty".equals(action)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = matched.get("candidates") instanceof List
                    ? (List<Map<String, Object>>) matched.get("candidates") : List.of();
            if (candidates != null && !candidates.isEmpty()) {
                String names = candidates.stream()
                        .map(c -> String.valueOf(c.get("dishName")))
                        .collect(Collectors.joining("、"));
                return "我说得有点飘，菜单里没完全对上，所以还没加进购物车。你看看这些像不像：" + names + "？回我菜名或去「点单」里点选就好～";
            }
            return "呜…菜单里没有对上的菜，我没敢假装加进购物车。去「点单」挑现有的，或告诉我菜单上的菜名，我再帮你加～";
        }
        if ("add_dishes".equals(action)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> added = matched.get("added") instanceof List
                    ? (List<Map<String, Object>>) matched.get("added") : List.of();
            if (added == null || added.isEmpty()) {
                return xiaobuWrapReply("add_dishes_empty", matched);
            }
            return "好～" + (base.isEmpty() ? "小布帮你加好菜了。" : base) + " 请到购物车确认呀～";
        }
        return base.isEmpty() ? "嗯嗯，小布听到啦～" : base;
    }

    private String callDeepSeekChat(String system, List<Map<String, String>> history,
                                    String user, double temperature) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        if (history != null) {
            for (Map<String, String> h : history) {
                if (h == null) continue;
                String role = h.get("role");
                String content = h.get("content");
                if (!StringUtils.hasText(role) || !StringUtils.hasText(content)) continue;
                if (!"user".equals(role) && !"assistant".equals(role)) continue;
                messages.add(Map.of("role", role, "content", content));
            }
        }
        messages.add(Map.of("role", "user", "content", user));
        body.put("messages", messages);

        String raw = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(timeoutSeconds + 5));

        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("DeepSeek 返回为空");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("DeepSeek 无 choices");
            }
            String content = choices.get(0).path("message").path("content").asText(null);
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("DeepSeek content 为空");
            }
            return content;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析 DeepSeek 响应失败", e);
        }
    }

    private static class MatchItem {
        Long dishId;
        Integer qty;
        double score;
        String reason;
    }
}