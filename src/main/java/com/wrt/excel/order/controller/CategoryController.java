package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.Category;
import com.wrt.excel.order.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/category")
@CrossOrigin(origins = "*")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    /**
     * 获取所有启用的分类
     */
    @GetMapping("/list")
    public Map<String, Object> getCategories() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Category> categories = categoryService.getAllActiveCategories();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", categories);
        } catch (Exception e) {
            log.error("获取分类失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取所有分类（包括禁用的）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllCategories() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Category> categories = categoryService.getAllCategories();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", categories);
        } catch (Exception e) {
            log.error("获取所有分类失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 添加分类
     */
    @PostMapping("/add")
    public Map<String, Object> addCategory(@RequestBody Category category) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = categoryService.addCategory(category);
            if (success) {
                result.put("code", 200);
                result.put("message", "添加分类成功");
            } else {
                result.put("code", 500);
                result.put("message", "添加分类失败");
            }
        } catch (Exception e) {
            log.error("添加分类失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 更新分类信息
     */
    @PostMapping("/update")
    public Map<String, Object> updateCategory(@RequestBody Category category) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = categoryService.updateCategory(category);
            if (success) {
                result.put("code", 200);
                result.put("message", "更新分类成功");
            } else {
                result.put("code", 500);
                result.put("message", "更新分类失败");
            }
        } catch (Exception e) {
            log.error("更新分类失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteCategory(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = categoryService.deleteCategory(id);
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "删除分类成功" : "删除分类失败");
        } catch (Exception e) {
            log.error("删除分类失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
