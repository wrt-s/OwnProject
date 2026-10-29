package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.DishDislike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户不喜欢菜品Mapper
 */
@Mapper
public interface DishDislikeMapper extends BaseMapper<DishDislike> {
}
