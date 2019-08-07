package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/5 14:17
 */
@Data
public class CpuInvestData {
    /**
     * 充值笔数
     */
    private long cpuInvestNotes;
    /**
     * 充值金额
     */
    private BigDecimal cpuInvestAmount;
    /**
     * 修正笔数
     */
    private long cpuInvestReviseNotes;
    /**
     * 修正金额
     */
    private BigDecimal cpuInvestReviseAmount;
}
