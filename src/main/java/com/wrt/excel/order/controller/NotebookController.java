package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.Notebook;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.NotebookService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记事本控制器（强制 Session 归属）
 */
@Slf4j
@RestController
@RequestMapping("/api/notebook")
public class NotebookController {
    
    @Autowired
    private NotebookService notebookService;

    private void assertFamilyAccess(Notebook item, HttpSession session) {
        if (item == null) {
            throw new IllegalArgumentException("笔记不存在");
        }
        AuthService.assertSameFamily(item.getFamilyId(), session);
    }
    
    @PostMapping("/add")
    public Map<String, Object> addNote(@RequestBody Notebook notebook, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.forceSessionUserId(session, notebook.getUserId());
            Long familyId = AuthService.requireFamilyId(session);
            notebook.setUserId(uid);
            notebook.setFamilyId(familyId);
            notebookService.addNote(notebook);
            result.put("code", 200);
            result.put("message", "添加笔记成功 📝");
        } catch (Exception e) {
            log.error("添加笔记失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/update")
    public Map<String, Object> updateNote(@RequestBody Notebook notebook, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            Notebook existing = notebookService.getNoteById(notebook.getId());
            assertFamilyAccess(existing, session);
            notebook.setUserId(existing.getUserId());
            notebook.setFamilyId(existing.getFamilyId());
            notebookService.updateNote(notebook);
            result.put("code", 200);
            result.put("message", "更新笔记成功 ✓");
        } catch (Exception e) {
            log.error("更新笔记失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteNote(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            assertFamilyAccess(notebookService.getNoteById(id), session);
            notebookService.deleteNote(id);
            result.put("code", 200);
            result.put("message", "删除笔记成功 🗑️");
        } catch (Exception e) {
            log.error("删除笔记失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/list")
    public Map<String, Object> getUserNotes(@RequestParam(required = false) Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<Notebook> notes = notebookService.getUserNotes(familyId);
            result.put("code", 200);
            result.put("data", notes);
            result.put("count", notes.size());
        } catch (Exception e) {
            log.error("获取笔记列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/detail/{id}")
    public Map<String, Object> getNoteDetail(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            Notebook note = notebookService.getNoteById(id);
            assertFamilyAccess(note, session);
            result.put("code", 200);
            result.put("data", note);
        } catch (Exception e) {
            log.error("获取笔记详情失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/search")
    public Map<String, Object> searchNotes(@RequestParam(required = false) Long userId,
                                           @RequestParam String keyword,
                                           HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<Notebook> notes = notebookService.searchNotes(familyId, keyword);
            result.put("code", 200);
            result.put("data", notes);
            result.put("count", notes.size());
        } catch (Exception e) {
            log.error("搜索笔记失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/category")
    public Map<String, Object> getNotesByCategory(@RequestParam(required = false) Long userId,
                                                   @RequestParam String category,
                                                   HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<Notebook> notes = notebookService.getNotesByCategory(familyId, category);
            result.put("code", 200);
            result.put("data", notes);
            result.put("count", notes.size());
        } catch (Exception e) {
            log.error("按分类获取笔记失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/pinned")
    public Map<String, Object> getPinnedNotes(@RequestParam(required = false) Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<Notebook> notes = notebookService.getPinnedNotes(familyId);
            result.put("code", 200);
            result.put("data", notes);
            result.put("count", notes.size());
        } catch (Exception e) {
            log.error("获取置顶笔记失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
