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
public class CpuInvest {

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
     * 交易时间<br>
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
     * 票价(元)<br>
     * 数据库字段:FEE
     */
    private BigDecimal FEE;

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
     * 附加交易类型(14 充值,其他是消费)<br>
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
     * 累计门槛月份<br>
     * 数据库字段:DMON
     */
    private String DMON;

    /**
     * 公交门槛计数<br>
     * 数据库字段:BDCT
     */
    private String BDCT;

    /**
     * 地铁门槛计数<br>
     * 数据库字段:MDCT
     */
    private String MDCT;

    /**
     * 联乘门槛计数<br>
     * 数据库字段:UDCT
     */
    private String UDCT;

    /**
     * 本次交易入口设备编号<br>
     * 数据库字段:EPID
     */
    private String EPID;

    /**
     * 本次交易入口日期时间<br>
     * 数据库字段:ETIM
     */
    private String ETIM;

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
     * 区域代码<br>
     * 数据库字段:AREA
     */
    private String AREA;

    /**
     * 区域卡类型<br>
     * 数据库字段:ACT
     */
    private String ACT;

    /**
     * 区域子码<br>
     * 数据库字段:SAREA
     */
    private String SAREA;

    /**
     * 交易认证码<br>
     * 数据库字段:TAC
     */
    private String TAC;

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