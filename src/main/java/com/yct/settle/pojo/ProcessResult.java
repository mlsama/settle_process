package com.yct.settle.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/10 9:17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessResult {

    /**
     * 清算日期<br>
     * 数据库字段:SETTLE_DATE
     */
    private String settleDate;
    /**
     * 开始处理的时间<br>
     * 数据库字段:STAER_TIME
     */
    private Date startTime;

    /**
     * 结束时间<br>
     * 数据库字段:END_TIME
     */
    private Date endTime;
    /**
     * 结果码(0000:成功)<br>
     * 数据库字段:RESULT_CODE
     */
    private String resultCode;

    /**
     * 结果描述<br>
     * 数据库字段:RESULT_MSG
     */
    private String resultMsg;

    /**
     * 总笔数
     */
    private long totalNotes ;
    /**
     * 总金额
     */
    private BigDecimal totalAmount ;
    
    /**
     * 充值总笔数
     */
    private long investNotes ;
    /**
     * 充值总金额
     */
    private BigDecimal investAmount ;
    /**
     * CPU充值笔数
     */
    private long cpuInvestNotes;
    /**
     * CPU充值金额
     */
    private BigDecimal cpuInvestAmount;
    /**
     * m1充值笔数
     */
    private long mCardInvestNotes;
    /**
     * m1充值金额
     */
    private BigDecimal mCardInvestAmount;
    /**
     * 消费总笔数
     */
    private long consumeNotes;
    /**
     * 消费总金额
     */
    private BigDecimal consumeAmount;
    /**
     * CPU消费笔数
     */
    private long cpuConsumeNotes;
    /**
     * CPU消费金额
     */
    private BigDecimal cpuConsumeAmount;
    /**
     * m1消费笔数
     */
    private long mCardConsumeNotes;
    /**
     * m1消费金额
     */
    private BigDecimal mCardConsumeAmount;

    /**
     * 客服总笔数
     */
    private long customerNotes;
    /**
     * 客服总金额
     */
    private BigDecimal customerAmount;
    /**
     * CPU客服笔数
     */
    private long cpuCustomerNotes;
    /**
     * CPU客服金额
     */
    private BigDecimal cpuCustomerAmount;
    /**
     * m1客服笔数
     */
    private long mCardCustomerNotes;
    /**
     * m1客服金额
     */
    private BigDecimal mCardCustomerAmount;

    /**
     * 修正笔数
     */
    private long reviseNotes;
    /**
     * 修正金额
     */
    private BigDecimal reviseAmount;
    /**
     * CPU修正笔数
     */
    private long cpuReviseNotes;
    /**
     * CPU修正金额
     */
    private BigDecimal cpuReviseAmount;
    /**
     * m1修正笔数
     */
    private long mCardReviseNotes;
    /**
     * m1修正金额
     */
    private BigDecimal mCardReviseAmount;
}
