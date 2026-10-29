package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.UserDailyBettingStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户每日押注统计Mapper
 */
@Mapper
public interface UserDailyBettingStatsMapper extends BaseMapper<UserDailyBettingStats> {
}
