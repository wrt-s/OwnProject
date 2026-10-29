package com.wrt.excel.order.controller;

import com.wrt.excel.order.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Session-backed cart sync. 小布 / AI add_dishes writes here so index can recover
 * even if a stale cached love.js wiped localStorage love_cart on history.back().
 */
@Slf4j
@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    public static final String SESSION_CART = "LOVE_CART_ITEMS";

    @GetMapping
    public Map<String, Object> getCart(HttpSession session) {
        AuthService.requireSessionUserId(session);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", Map.of("items", snapshot(session)));
        return result;
    }

    @PostMapping("/sync")
    @SuppressWarnings("unchecked")
    public Map<String, Object> syncCart(@RequestBody Map<String, Object> body, HttpSession session) {
        AuthService.requireSessionUserId(session);
        Object itemsObj = body == null ? null : body.get("items");
        List<Map<String, Object>> items = normalizeList(itemsObj);
        session.setAttribute(SESSION_CART, items);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "ok");
        result.put("data", Map.of("items", items, "count", items.size()));
        return result;
    }

    @PostMapping("/clear")
    public Map<String, Object> clearCart(HttpSession session) {
        AuthService.requireSessionUserId(session);
        session.setAttribute(SESSION_CART, new ArrayList<Map<String, Object>>());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "cleared");
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> snapshot(HttpSession session) {
        Object raw = session.getAttribute(SESSION_CART);
        if (!(raw instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                out.add(normalizeItem((Map<String, Object>) m));
            }
        }
        out.removeIf(Objects::isNull);
        return out;
    }

    /**
     * @return number of added items successfully merged into session cart (0 = durable write failed)
     */
    @SuppressWarnings("unchecked")
    public static int mergeAdded(HttpSession session, List<?> added) {
        List<Map<String, Object>> cart = new ArrayList<>(snapshot(session));
        if (added == null || added.isEmpty()) {
            session.setAttribute(SESSION_CART, cart);
            return 0;
        }
        int merged = 0;
        for (Object o : added) {
            if (!(o instanceof Map<?, ?>)) continue;
            Map<String, Object> item = normalizeItem((Map<String, Object>) o);
            if (item == null) continue;
            Long id = ((Number) item.get("dishId")).longValue();
            boolean found = false;
            for (Map<String, Object> existing : cart) {
                if (((Number) existing.get("dishId")).longValue() == id) {
                    int q = ((Number) existing.get("quantity")).intValue()
                            + ((Number) item.get("quantity")).intValue();
                    existing.put("quantity", Math.min(99, q));
                    if (item.get("dishName") != null) existing.put("dishName", item.get("dishName"));
                    if (item.get("price") != null) existing.put("price", item.get("price"));
                    if (item.get("priceType") != null) existing.put("priceType", item.get("priceType"));
                    found = true;
                    break;
                }
            }
            if (!found) cart.add(item);
            merged++;
        }
        session.setAttribute(SESSION_CART, cart);
        return merged;
    }

    public static void clear(HttpSession session) {
        session.setAttribute(SESSION_CART, new ArrayList<Map<String, Object>>());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> normalizeList(Object itemsObj) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (!(itemsObj instanceof List<?> list)) return items;
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                Map<String, Object> n = normalizeItem((Map<String, Object>) m);
                if (n != null) items.add(n);
            }
        }
        return items;
    }

    private static Map<String, Object> normalizeItem(Map<String, Object> it) {
        if (it == null) return null;
        Object rawId = it.get("dishId") != null ? it.get("dishId") : it.get("id");
        if (rawId == null) return null;
        long id;
        try { id = Long.parseLong(String.valueOf(rawId)); } catch (Exception e) { return null; }
        if (id <= 0) return null;
        int qty = 1;
        try {
            Object q = it.get("quantity") != null ? it.get("quantity") : it.get("qty");
            if (q != null) qty = Math.max(1, Math.min(99, Integer.parseInt(String.valueOf(q))));
        } catch (Exception ignored) {}
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dishId", id);
        Object name = it.get("dishName") != null ? it.get("dishName") : it.get("name");
        row.put("dishName", name == null ? ("#" + id) : String.valueOf(name));
        row.put("price", it.get("price") == null ? 0 : it.get("price"));
        row.put("priceType", it.get("priceType") == null ? "爱心币" : String.valueOf(it.get("priceType")));
        row.put("quantity", qty);
        row.put("remark", it.get("remark") == null ? "" : String.valueOf(it.get("remark")));
        return row;
    }
}
