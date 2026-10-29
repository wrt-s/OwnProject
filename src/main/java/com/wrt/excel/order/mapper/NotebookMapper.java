package com.wrt.excel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrt.excel.order.entity.Notebook;
import org.apache.ibatis.annotations.Mapper;

/**
 * 记事本Mapper - 一二的小本本
 */
@Mapper
public interface NotebookMapper extends BaseMapper<Notebook> {
}
