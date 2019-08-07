package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/5 14:59
 */
@Data
public class CpuConsumerData {
    /**
     * 消费笔数
     */
    private long cpuConsumeNotes;
    /**
     * 消费金额
     */
    private BigDecimal cpuConsumeAmount;
    /**
     * 修正笔数
     */
    private long cpuConsumerReviseNotes;
    /**
     * 修正金额
     */
    private BigDecimal cpuConsumerReviseAmount;
}
