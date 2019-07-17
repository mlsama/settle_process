package com.yct.settle.pojo;

import com.yct.settle.utils.AmountUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MCardTrade {
    /**
     * 清算日期
     */
    private String QDATE;
    /**
     * 数据类型:01 充值 02 消费 03 客服
     */
    private String DT;
    /**
     * 区域代码
     */
    private String ISSUEA;
    /**
     * 使用地（区域代码）
     */
    private String USEA;
    /**
     * 脱机交易流水号
     */
    private String DSN;
    /**
     * 票卡逻辑卡号
     */
    private String ICN;
    /**
     * 票卡物理卡号
     */
    private String FCN;
    /**
     * 上次交易设备编号
     */
    private String LPID;
    /**
     * 上次交易日期时间
     */
    private String LTIM;
    /**
     * 本次交易设备编号
     */
    private String PID;
    /**
     * 本次交易日期时间
     */
    private String TIM;
    /**
     * 交易金额（实扣）
     */
    private BigDecimal TF;
    /**
     * 余额
     */
    private BigDecimal BAL;
    /**
     * 票价（应扣）
     */
    private BigDecimal FEE;
    /**
     * 交易类型:原数据交易类型
     */
    private String TT;
    /**
     * 票卡交易计数
     */
    private String RN;
    /**
     * 累计门槛月份
     */
    private String DMON;
    /**
     * 公交门槛计数
     */
    private String BDCT;
    /**
     * 地铁门槛计数
     */
    private String MDCT;
    /**
     * 联乘门槛计数
     */
    private String UDCT;
    /**
     * 本次交易入口设备编号
     */
    private String EPID;
    /**
     * 本次交易入口日期时间
     */
    private String ETIM;
    /**
     * 交易认证码
     */
    private String TAC;
    /**
     * 备用信息（充值：账户类型+18个0，消费：20个0）
     */
    private String BINF;
    /**
     * 上传清算文件名
     */
    private String QNAME;

    private String FLAG;

    public String toString() {
        return QDATE+'\t'+DT + '\t'+"01"+ '\t' + "01" + '\t'+ DSN + '\t'+ ICN + '\t'+ FCN + '\t'+ LPID + '\t'
                + LTIM + '\t'+ PID +'\t' + TIM +'\t' + AmountUtil.convertToString(TF, 8) +'\t' +
                AmountUtil.convertToString(BAL, 8) + '\t' + AmountUtil.convertToString(FEE, 8) + '\t' +
                TT +'\t' + RN +'\t' + "0000" + '\t' + "000" + '\t' + "000" + '\t' + "000" + '\t' + EPID + '\t' +
                ETIM + '\t' + TAC + '\t' + BINF + '\t' + QNAME;
    }
}
