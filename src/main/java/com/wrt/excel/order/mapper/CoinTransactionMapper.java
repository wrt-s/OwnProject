package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.CoinTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 爱心币流水Mapper
 */
@Mapper
public interface CoinTransactionMapper extends BaseMapper<CoinTransaction> {
}
