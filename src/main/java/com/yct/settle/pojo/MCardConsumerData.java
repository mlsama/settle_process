package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/5 14:59
 */
@Data
public class MCardConsumerData {
    /**
     * 消费笔数
     */
    private long mCardConsumeNotes;
    /**
     * 消费金额
     */
    private BigDecimal mCardConsumeAmount;
    /**
     * 修正笔数
     */
    private long mCardConsumerReviseNotes;
    /**
     * 修正金额
     */
    private BigDecimal mCardConsumerReviseAmount;
}
