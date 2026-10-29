package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.TodoItem;
import com.wrt.excel.order.mapper.TodoItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办事项服务
 */
@Slf4j
@Service
public class TodoService {
    
    @Autowired
    private TodoItemMapper todoItemMapper;
    
    @Autowired
    private ServerChanService serverChanService;
    
    /**
     * 添加待办事项
     */
    public TodoItem getById(Long id) {
        return todoItemMapper.selectById(id);
    }
    
    public void addTodo(TodoItem todoItem) {
        if (todoItem.getStatus() == null) {
            todoItem.setStatus("pending");
        }
        if (todoItem.getPriority() == null) {
            todoItem.setPriority("medium");
        }
        todoItemMapper.insert(todoItem);
        log.info("添加待办事项: {}", todoItem.getTitle());
    }
    
    /**
     * 更新待办事项
     */
    public void updateTodo(TodoItem todoItem) {
        todoItemMapper.updateById(todoItem);
        log.info("更新待办事项: {}", todoItem.getId());
    }
    
    /**
     * 删除待办事项
     */
    public void deleteTodo(Long id) {
        todoItemMapper.deleteById(id);
        log.info("删除待办事项: {}", id);
    }
    
    /**
     * 获取用户所有待办事项
     */
    public List<TodoItem> getUserTodos(Long familyId) {
        LambdaQueryWrapper<TodoItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TodoItem::getFamilyId, familyId)
               .orderByAsc(TodoItem::getDueDate)  // 按截止日期升序排列（最近的在前）
               .orderByDesc(TodoItem::getCreatedAt);  // 如果截止日期相同，按创建时间降序
        
        return todoItemMapper.selectList(wrapper);
    }
    
    /**
     * 根据状态获取待办事项
     */
    public List<TodoItem> getTodosByStatus(Long familyId, String status) {
        LambdaQueryWrapper<TodoItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TodoItem::getFamilyId, familyId)
               .eq(TodoItem::getStatus, status)
               .orderByAsc(TodoItem::getDueDate)  // 按截止日期升序排列（最近的在前）
               .orderByDesc(TodoItem::getCreatedAt);  // 如果截止日期相同，按创建时间降序
        
        return todoItemMapper.selectList(wrapper);
    }
    
    /**
     * 根据负责人获取待办事项
     */
    public List<TodoItem> getTodosByAssignee(Long familyId, String assignee) {
        LambdaQueryWrapper<TodoItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TodoItem::getFamilyId, familyId)
               .eq(TodoItem::getAssignee, assignee)
               .orderByAsc(TodoItem::getDueDate)  // 按截止日期升序排列（最近的在前）
               .orderByDesc(TodoItem::getCreatedAt);  // 如果截止日期相同，按创建时间降序
        
        return todoItemMapper.selectList(wrapper);
    }
    
    /**
     * 完成待办事项
     */
    public void completeTodo(Long id) {
        TodoItem todoItem = todoItemMapper.selectById(id);
        if (todoItem != null) {
            todoItem.setStatus("completed");
            todoItem.setCompletedAt(LocalDateTime.now());
            todoItemMapper.updateById(todoItem);
            log.info("完成待办事项: {}", id);
        }
    }
    
    /**
     * 重新打开待办事项
     */
    public void reopenTodo(Long id) {
        TodoItem todoItem = todoItemMapper.selectById(id);
        if (todoItem != null) {
            todoItem.setStatus("pending");
            todoItem.setCompletedAt(null);
            todoItemMapper.updateById(todoItem);
            log.info("重新打开待办事项: {}", id);
        }
    }
    
    /**
     * 统计待办事项数量
     */
    public long countTodosByStatus(Long familyId, String status) {
        LambdaQueryWrapper<TodoItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TodoItem::getFamilyId, familyId)
               .eq(TodoItem::getStatus, status);
        
        return todoItemMapper.selectCount(wrapper);
    }
    
    /**
     * 发送待办提醒
     */
    public boolean sendReminder(Long id) {
        TodoItem todoItem = todoItemMapper.selectById(id);
        if (todoItem == null) {
            log.warn("待办事项不存在: {}", id);
            return false;
        }
        
        // 检查是否已完成
        if ("completed".equals(todoItem.getStatus())) {
            log.warn("待办事项已完成，无需提醒: {}", id);
            return false;
        }
        
        // 发送Server酱通知
        String dueDateStr = todoItem.getDueDate() != null ? todoItem.getDueDate().toString() : "未设置";
        boolean success = serverChanService.sendTodoReminder(
            todoItem.getTitle(),
            todoItem.getAssignee(),
            dueDateStr,
            todoItem.getPriority()
        );
        
        if (success) {
            // 更新提醒状态
            todoItem.setReminderStatus("sent");
            todoItem.setReminderTime(LocalDateTime.now());
            todoItemMapper.updateById(todoItem);
            log.info("待办提醒发送成功: {}", id);
        } else {
            log.error("待办提醒发送失败: {}", id);
        }
        
        return success;
    }
}
