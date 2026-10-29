package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.DailyCheckin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日签到记录Mapper
 */
@Mapper
public interface DailyCheckinMapper extends BaseMapper<DailyCheckin> {
}
