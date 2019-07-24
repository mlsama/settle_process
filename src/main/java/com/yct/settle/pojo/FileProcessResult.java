/**
 * <html>
 *  <body>
 *   <P> Copyright 2018 广东粤通宝电子商务有限公司 </p>
 *   <p> All rights reserved.</p>
 *   <p> Created on 2019年7月6日</p>
 *   <p> Created by mlsama</p>
 *  </body>
 * </html>
 */
package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FileProcessResult{

    public FileProcessResult(){}

    public FileProcessResult(String zipFileName, Date endTime, String resultCode, String resultMsg,
                             long investNotes, BigDecimal investAmount, long consumeNotes,
                             BigDecimal consumeAmount, long reviseNotes, BigDecimal reviseAmount) {
        this.zipFileName = zipFileName;
        this.endTime = endTime;
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
        this.investNotes = investNotes;
        this.investAmount = investAmount;
        this.consumeNotes = consumeNotes;
        this.consumeAmount = consumeAmount;
        this.reviseNotes = reviseNotes;
        this.reviseAmount = reviseAmount;
    }

    public FileProcessResult(String zipFileName, String errorFileName, Date endTime, String resultCode, String resultMsg) {
        this.zipFileName = zipFileName;
        this.errorFileName = errorFileName;
        this.endTime = endTime;
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
    }

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
     * 异常的文件<br>
     * 数据库字段:ERROR_FILE_NAME
     */
    private String errorFileName;

    /**
     * 开始处理的时间<br>
     * 数据库字段:START_TIME
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
     * 充值笔数<br>
     * 数据库字段:INVEST_NOTES
     */
    private long investNotes;

    /**
     * 充值金额<br>
     * 数据库字段:INVEST_AMOUNT
     */
    private BigDecimal investAmount;

    /**
     * 消费笔数<br>
     * 数据库字段:CONSUME_NOTES
     */
    private long consumeNotes;

    /**
     * 消费金额<br>
     * 数据库字段:CONSUME_AMOUNT
     */
    private BigDecimal consumeAmount;
    /**
     * 修正笔数<br>
     * 数据库字段:REVISE_NOTES
     */
    private long reviseNotes;

    /**
     * 修正金额<br>
     * 数据库字段:REVISE_AMOUNT
     */
    private BigDecimal reviseAmount;

}