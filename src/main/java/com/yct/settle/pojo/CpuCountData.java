package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CpuCountData {
    /**
     * 充值笔数
     */
    private int investNotes;
    /**
     * 充值总金额
     */
    private BigDecimal investAmount;
    /**
     * 消费笔数
     */
    private int consumeNotes;
    /**
     * 消费总金额
     */
    private BigDecimal consumeAmount;
    /**
     * 修正笔数
     */
    private int reviseNotes;
    /**
     * 修正总金额
     */
    private BigDecimal reviseAmount;
}
