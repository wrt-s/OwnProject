package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.TodoItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待办事项Mapper
 */
@Mapper
public interface TodoItemMapper extends BaseMapper<TodoItem> {
}
