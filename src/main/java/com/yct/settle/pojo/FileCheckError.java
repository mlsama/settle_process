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

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
public class FileCheckError {

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
     * 校验时间  <br>
     * 数据库字段:CHECK_DATE
     */
    private Date checkDate;

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
     * input充值笔数<br>
     */
    private long inputInvestNotes;

    /**
     * input充值金额<br>
     */
    private BigDecimal inputInvestAmount;
    /**
     * output充值笔数<br>
     */
    private long outputInvestNotes;

    /**
     * output充值金额<br>
     */
    private BigDecimal outputInvestAmount;

    /**
     * input错误消费笔数<br>
     */
    private long inputErrorNotes;

    /**
     * output错误消费笔数<br>
     */
    private long outputErrorNotes;

    /**
     * 正确消费总金额
     */
    private BigDecimal consumeAmount;

    /**
     * 清算文件总金额
     */
    private BigDecimal QsAmount;

    public FileCheckError(String settleDate, String zipFileName, String zipFileType, String cardType, Date checkDate, String resultCode, String resultMsg) {
        this.settleDate = settleDate;
        this.zipFileName = zipFileName;
        this.zipFileType = zipFileType;
        this.cardType = cardType;
        this.checkDate = checkDate;
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
    }
}