package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.TodoItem;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.TodoService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办事项控制器（写操作强制 Session 用户；按 id 变更需校验归属）
 */
@Slf4j
@RestController
@RequestMapping("/api/todo")
public class TodoController {
    
    @Autowired
    private TodoService todoService;

    private void assertFamilyAccess(TodoItem item, HttpSession session) {
        if (item == null) {
            throw new IllegalArgumentException("待办不存在");
        }
        AuthService.assertSameFamily(item.getFamilyId(), session);
    }
    
    @PostMapping("/add")
    public Map<String, Object> addTodo(@RequestBody TodoItem todoItem, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.forceSessionUserId(session, todoItem.getUserId());
            Long familyId = AuthService.requireFamilyId(session);
            todoItem.setUserId(uid);
            todoItem.setFamilyId(familyId);
            todoService.addTodo(todoItem);
            result.put("code", 200);
            result.put("message", "添加待办成功 ✓");
        } catch (Exception e) {
            log.error("添加待办失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/update")
    public Map<String, Object> updateTodo(@RequestBody TodoItem todoItem, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            TodoItem existing = todoService.getById(todoItem.getId());
            assertFamilyAccess(existing, session);
            todoItem.setUserId(existing.getUserId());
            todoItem.setFamilyId(existing.getFamilyId());
            todoService.updateTodo(todoItem);
            result.put("code", 200);
            result.put("message", "更新待办成功");
        } catch (Exception e) {
            log.error("更新待办失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteTodo(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            assertFamilyAccess(todoService.getById(id), session);
            todoService.deleteTodo(id);
            result.put("code", 200);
            result.put("message", "删除待办成功");
        } catch (Exception e) {
            log.error("删除待办失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/list/{userId}")
    public Map<String, Object> getUserTodos(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<TodoItem> todos = todoService.getUserTodos(familyId);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", todos);
        } catch (Exception e) {
            log.error("获取待办列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/list/{userId}/{status}")
    public Map<String, Object> getTodosByStatus(@PathVariable Long userId, @PathVariable String status, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            if ("done".equalsIgnoreCase(status)) status = "completed";
            List<TodoItem> todos = todoService.getTodosByStatus(familyId, status);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", todos);
        } catch (Exception e) {
            log.error("获取待办列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/complete/{id}")
    public Map<String, Object> completeTodo(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            assertFamilyAccess(todoService.getById(id), session);
            todoService.completeTodo(id);
            result.put("code", 200);
            result.put("message", "恭喜完成！🎉");
        } catch (Exception e) {
            log.error("完成待办失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/reopen/{id}")
    public Map<String, Object> reopenTodo(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            assertFamilyAccess(todoService.getById(id), session);
            todoService.reopenTodo(id);
            result.put("code", 200);
            result.put("message", "已重新打开");
        } catch (Exception e) {
            log.error("重新打开待办失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/remind/{id}")
    public Map<String, Object> remindTodo(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            assertFamilyAccess(todoService.getById(id), session);
            boolean success = todoService.sendReminder(id);
            if (success) {
                result.put("code", 200);
                result.put("message", "提醒已发送 📬");
            } else {
                result.put("code", 500);
                result.put("message", "提醒发送失败，请检查配置");
            }
        } catch (Exception e) {
            log.error("发送待办提醒失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
