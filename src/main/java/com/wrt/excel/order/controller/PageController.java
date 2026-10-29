package com.wrt.excel.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器
 */
@Slf4j
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Legacy /orders bookmark → home orders tab */
    @GetMapping("/orders")
    public String ordersRedirect() {
        return "redirect:/?tab=orders";
    }

    /** H5 登录页 */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/order-history")
    public String orderHistory() {
        return "order-history";
    }

    @GetMapping("/coin-management")
    public String coinManagement() {
        return "coin-management";
    }

    @GetMapping("/favorites")
    public String favorites() {
        return "favorites";
    }

    @GetMapping("/anniversaries")
    public String anniversaries() {
        return "anniversaries";
    }

    @GetMapping("/todos")
    public String todos() {
        return "todos";
    }

    @GetMapping("/achievements")
    public String achievements() {
        return "achievements";
    }

    @GetMapping("/checkin")
    public String checkin() {
        return "checkin";
    }

    @GetMapping("/lottery")
    public String lottery() {
        return "lottery";
    }

    @GetMapping("/betting")
    public String betting() {
        return "betting";
    }

    @GetMapping("/feeding-game")
    public String feedingGame() {
        return "feeding-game";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/notebook")
    public String notebook() {
        return "notebook";
    }

    @GetMapping("/mood")
    public String mood() {
        return "mood";
    }

    @GetMapping("/ai-chat")
    public String aiChat() {
        return "ai-chat";
    }

    @GetMapping("/blindbox")
    public String blindbox() {
        return "blindbox";
    }

    @GetMapping("/family-location")
    public String familyLocation() {
        return "family-location";
    }
}