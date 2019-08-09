package com.yct.settle.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DESC: 异常修正数据
 * AUTHOR:mlsama
 * 2019/8/7 16:23
 */
@Data
public class ExceptionRevise {

    private long errorNotes;
    private BigDecimal errorAmount;
    private long exceptionNotes;
    private BigDecimal exceptionAmount;

    private long notes;
    private BigDecimal amount;
}
