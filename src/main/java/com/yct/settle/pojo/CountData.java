package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/4 9:17
 */
@Data
public class CountData {
    /**
     * 总笔数
     */
    private int notesSum;
    /**
     * 总金额
     */
    private BigDecimal amountSum;

    /**
     * 充值笔数
     */
    private int investNotes;
    /**
     * 充值金额
     */
    private BigDecimal investAmount;
    /**
     * 消费笔数
     */
    private int consumeNotes;
    /**
     * 消费金额
     */
    private BigDecimal consumeAmount;
    /**
     * 客服笔数
     */
    private int customerNotes;
    /**
     * 客服金额
     */
    private BigDecimal customerAmount;
    /**
     * 修正笔数
     */
    private int reviseNotes;
    /**
     * 修正金额
     */
    private BigDecimal reviseAmount;
}
