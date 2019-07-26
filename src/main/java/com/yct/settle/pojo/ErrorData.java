package com.yct.settle.pojo;

import lombok.Data;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/26 16:09
 */
@Data
public class ErrorData {

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
