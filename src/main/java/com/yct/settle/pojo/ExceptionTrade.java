package com.yct.settle.pojo;

import lombok.Data;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/8 15:46
 */
@Data
public class ExceptionTrade {
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
}
