package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.DishFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品收藏Mapper
 */
@Mapper
public interface DishFavoriteMapper extends BaseMapper<DishFavorite> {
}
