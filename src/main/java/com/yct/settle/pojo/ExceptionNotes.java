package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/26 16:06
 */
@Data
public class ExceptionNotes {

    /**
     * 清算日期<br>
     * 数据库字段:SETTLE_DATE
     */
    private String settleDate;

    /**
     * 清算压缩文件包<br>
     * 数据库字段:ZIP_FILE_NAME
     */
    private String zipFileName;

    /**
     * 清算文件类型（01：充值，02：消费，03：客服）<br>
     * 数据库字段:ZIP_FILE_TYPE
     */
    private String zipFileType;

    /**
     * 卡类型（01：cpu卡，02：M1卡）<br>
     * 数据库字段:CARD_TYPE
     */
    private String cardType;

    /**
     * 本次交易设备编号<br>
     * 数据库字段:PID
     */
    private String PID;

    /**
     * 脱机交易流水号<br>
     * 数据库字段:PSN
     */
    private String PSN;

    /**
     * 本次交易日期时间<br>
     * 数据库字段:TIM
     */
    private String TIM;

    /**
     * 票卡号<br>
     * 数据库字段:LCN
     */
    private String LCN;

    /**
     * 票卡物理卡号<br>
     * 数据库字段:FCN
     */
    private String FCN;

    /**
     * 交易金额(元)<br>
     * 数据库字段:TF
     */
    private BigDecimal TF;

    /**
     * 余额(元)<br>
     * 数据库字段:BAL
     */
    private BigDecimal BAL;

    /**
     * 交易类型(02 充值,06 消费,09 复合消费)<br>
     * 数据库字段:TT
     */
    private String TT;

    /**
     * 交易认证码<br>
     * 数据库字段:TAC
     */
    private String TAC;


}
