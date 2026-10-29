package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Category;
import com.wrt.excel.order.mapper.CategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务
 */
@Slf4j
@Service
public class CategoryService {
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    /**
     * 获取所有启用的分类
     */
    public List<Category> getAllActiveCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getIsActive, 1)
               .orderByAsc(Category::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }
    
    /**
     * 获取所有分类
     */
    public List<Category> getAllCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }
    
    /**
     * 添加分类
     */
    public boolean addCategory(Category category) {
        try {
            int result = categoryMapper.insert(category);
            return result > 0;
        } catch (Exception e) {
            log.error("添加分类失败", e);
            return false;
        }
    }
    
    /**
     * 更新分类信息
     */
    public boolean updateCategory(Category category) {
        try {
            int result = categoryMapper.updateById(category);
            return result > 0;
        } catch (Exception e) {
            log.error("更新分类失败", e);
            return false;
        }
    }

    public boolean deleteCategory(Long id) {
        return this.categoryMapper.deleteById(id) > 0;
    }
}
