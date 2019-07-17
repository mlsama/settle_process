package com.yct.settle.pojo;

import com.yct.settle.utils.AmountUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DESC: CJ-CPU钱包明细
 * AUTHOR:mlsama
 * 2019/6/27 18:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CpuTrade {
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
     * 本次交易设备编号
     */
    private String PID;
    /**
     * 脱机交易流水号
     */
    private String PSN;
    /**
     * 本次交易日期时间
     */
    private String TIM;
    /**
     * 票卡逻辑卡号
     */
    private String LCN;
    /**
     * 票卡物理卡号
     */
    private String FCN;
    /**
     * 交易金额（实扣）
     */
    private BigDecimal TF;
    /**
     * 票价（应扣）
     */
    private BigDecimal FEE;
    /**
     * 余额
     */
    private BigDecimal BAL;
    /**
     * 交易类型
     */
    private String TT;
    /**
     * 附加交易类型
     */
    private String ATT;
    /**
     * 票卡充值交易计数
     */
    private String CRN;
    /**
     * 票卡消费交易计数
     */
    private String XRN;
    /**
     * 扩展信息
     */
    private String DMON;
    /**
     * 本次交易入口设备编号
     */
    private String EPID;
    /**
     * 本次交易入口日期时间
     */
    private String ETIM;
    /**
     * 上次交易设备编号
     */
    private String LPID;
    /**
     * 上次交易日期时间
     */
    private String LTIM;
    /**
     * 交易认证码
     */
    private String TAC;
    /**
     * 上传清算文件名
     */
    private String QNAME;

    private String FLAG;

    public String toString() {
        if (LTIM.length() == 8){
            LTIM += 00;
        }
        return QDATE+'\t'+DT + '\t'+"01"+ '\t' + "01" + '\t'+ PID + '\t'+ PSN + '\t'+ TIM + '\t'+ LCN + '\t'
                + FCN + '\t'+ AmountUtil.convertToString(TF, 8) +'\t' + AmountUtil.convertToString(FEE, 8)
                +'\t' + AmountUtil.convertToString(BAL, 8) +'\t' + TT + '\t' + ATT + '\t' + CRN +'\t' + XRN +'\t'
                + DMON + '\t' + EPID + '\t' + ETIM + '\t' + LPID + '\t' + LTIM + '\t' + TAC + '\t' + QNAME ;
    }
}
