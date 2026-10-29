package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
