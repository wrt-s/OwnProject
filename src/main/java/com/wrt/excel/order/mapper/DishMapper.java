package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品Mapper
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
