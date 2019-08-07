package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/5 14:48
 */
@Data
public class MCardInvestData {
    /**
     * 充值笔数
     */
    private long mCardInvestNotes;
    /**
     * 充值金额
     */
    private BigDecimal mCardInvestAmount;
    /**
     * 修正笔数
     */
    private long mCardInvestReviseNotes;
    /**
     * 修正金额
     */
    private BigDecimal mCardInvestReviseAmount;
}
