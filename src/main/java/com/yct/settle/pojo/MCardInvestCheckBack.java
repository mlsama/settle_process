/**
 * <html>
 *  <body>
 *   <P> Copyright 2018 广东粤通宝电子商务有限公司 </p>
 *   <p> All rights reserved.</p>
 *   <p> Created on 2019年7月5日</p>
 *   <p> Created by mlsama</p>
 *  </body>
 * </html>
 */
package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class MCardInvestCheckBack  {

    /**
     * 脱机交易流水号<br>
     * 数据库字段:PSN
     */
    private String PSN;

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
     * 上次交易设备编号<br>
     * 数据库字段:LPID
     */
    private String LPID;

    /**
     * 上次交易日期时间<br>
     * 数据库字段:LTIM
     */
    private String LTIM;

    /**
     * 本次交易设备编号<br>
     * 数据库字段:PID
     */
    private String PID;

    /**
     * 本次交易日期时间<br>
     * 数据库字段:TIM
     */
    private String TIM;

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
     * 交易类型<br>
     * 数据库字段:TT
     */
    private String TT;

    /**
     * 票卡交易计数<br>
     * 数据库字段:RN
     */
    private String RN;

    /**
     * 账户类型<br>
     * 数据库字段:APP
     */
    private String APP;

    /**
     * 审核标志<br>
     * 数据库字段:FLAG
     */
    private String FLAG;

    /**
     * 数据异常代码<br>
     * 数据库字段:ERRNO
     */
    private String ERRNO;

}