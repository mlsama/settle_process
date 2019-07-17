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
public class CpuInvestCheckBack  {

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
     * 交易金额<br>
     * 数据库字段:TF
     */
    private BigDecimal TF;

    /**
     * 票价<br>
     * 数据库字段:FEE
     */
    private BigDecimal FEE;

    /**
     * 余额<br>
     * 数据库字段:BAL
     */
    private BigDecimal BAL;

    /**
     * 交易类型<br>
     * 数据库字段:TT
     */
    private String TT;

    /**
     * 附加交易类型<br>
     * 数据库字段:ATT
     */
    private String ATT;

    /**
     * 票卡充值交易计数<br>
     * 数据库字段:CRN
     */
    private String CRN;

    /**
     * 票卡消费交易计数<br>
     * 数据库字段:XRN
     */
    private String XRN;

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
     * 账户类型<br>
     * 数据库字段:APP
     */
    private String APP;

    /**
     * 清算返回标志<br>
     * 数据库字段:FLAG
     */
    private String FLAG;

    /**
     * 错误代码<br>
     * 数据库字段:ERRNO
     */
    private String ERRNO;

}