package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订单实体
 */
@Data
@TableName("orders")
public class Order implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 总价格
     */
    private Integer totalPrice;
    
    /**
     * 价格类型：单一币种（爱心币/亲亲/抱抱）或「多币种」。
     * 金额明细见 {@link #priceSummary} / {@link #currencyTotals}（不落库）。
     */
    private String priceType;

    /**
     * 分币种合计，例如「10 爱心币 + 2 亲亲」。仅接口返回，不写库。
     */
    @TableField(exist = false)
    private String priceSummary;

    /**
     * 分币种金额。仅接口返回，不写库。
     */
    @TableField(exist = false)
    private Map<String, Integer> currencyTotals;
    
    /**
     * 状态：pending/cooking/ready/completed/cancelled（旧值 confirmed/received→pending，preparing→cooking，delivered→ready）
     */
    private String status;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 特殊消息（如生日祝福）
     */
    private String specialMessage;
    
    /**
     * 下单时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
