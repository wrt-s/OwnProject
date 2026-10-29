package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Notebook;
import com.wrt.excel.order.mapper.NotebookMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记事本服务 - 一二的小本本
 */
@Slf4j
@Service
public class NotebookService {
    
    @Autowired
    private NotebookMapper notebookMapper;
    
    /**
     * 添加笔记
     */
    public void addNote(Notebook notebook) {
        if (notebook.getIsPinned() == null) {
            notebook.setIsPinned(false);
        }
        LocalDateTime now = LocalDateTime.now();
        notebook.setCreatedAt(now);
        notebook.setUpdatedAt(now);
        notebookMapper.insert(notebook);
        log.info("添加笔记: {}", notebook.getTitle());
    }
    
    /**
     * 更新笔记
     */
    public void updateNote(Notebook notebook) {
        notebook.setUpdatedAt(LocalDateTime.now());
        notebookMapper.updateById(notebook);
        log.info("更新笔记: {}", notebook.getId());
    }
    
    /**
     * 删除笔记
     */
    public void deleteNote(Long id) {
        notebookMapper.deleteById(id);
        log.info("删除笔记: {}", id);
    }
    
    /**
     * 获取用户所有笔记
     */
    public List<Notebook> getUserNotes(Long familyId) {
        LambdaQueryWrapper<Notebook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notebook::getFamilyId, familyId)
               .orderByDesc(Notebook::getIsPinned)
               .orderByDesc(Notebook::getUpdatedAt);
        
        return notebookMapper.selectList(wrapper);
    }
    
    /**
     * 根据ID获取笔记
     */
    public Notebook getNoteById(Long id) {
        return notebookMapper.selectById(id);
    }
    
    /**
     * 搜索笔记
     */
    public List<Notebook> searchNotes(Long familyId, String keyword) {
        LambdaQueryWrapper<Notebook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notebook::getFamilyId, familyId)
               .and(w -> w.like(Notebook::getTitle, keyword)
                         .or()
                         .like(Notebook::getContent, keyword))
               .orderByDesc(Notebook::getUpdatedAt);
        
        return notebookMapper.selectList(wrapper);
    }
    
    /**
     * 按分类获取笔记
     */
    public List<Notebook> getNotesByCategory(Long familyId, String category) {
        LambdaQueryWrapper<Notebook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notebook::getFamilyId, familyId)
               .eq(Notebook::getCategory, category)
               .orderByDesc(Notebook::getIsPinned)
               .orderByDesc(Notebook::getUpdatedAt);
        
        return notebookMapper.selectList(wrapper);
    }
    
    /**
     * 获取置顶笔记
     */
    public List<Notebook> getPinnedNotes(Long familyId) {
        LambdaQueryWrapper<Notebook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notebook::getFamilyId, familyId)
               .eq(Notebook::getIsPinned, true)
               .orderByDesc(Notebook::getUpdatedAt);
        
        return notebookMapper.selectList(wrapper);
    }
}
