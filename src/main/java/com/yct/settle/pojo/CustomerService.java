package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC: 客服
 * AUTHOR:mlsama
 * 2019/8/2 15:05
 */
@Data
public class CustomerService {

    private Long cpuCustomerInvestNotes;

    private BigDecimal cpuCustomerInvestAmount;

    private Long cpuCustomerConsumerNotes;

    private BigDecimal cpuCustomerConsumerAmount;

    private Long mCardCustomerInvestNotes;

    private BigDecimal mCardCustomerInvestAmount;

    private Long mCardCustomerConsumerNotes;

    private BigDecimal mCardCustomerConsumerAmount;
}
