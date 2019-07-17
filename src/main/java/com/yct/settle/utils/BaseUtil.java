package com.yct.settle.utils;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/5 14:07
 */
public class BaseUtil {


    public static Long convertLong(Object obj){
        if (obj != null){
            return (Long)obj;
        }
        return 0L;
    }
    public static BigDecimal convertBigdecimal(Object obj){
        if (obj != null){
            return (BigDecimal)obj;
        }
        return new BigDecimal("0");
    }
    public static String convertString(Object obj){
        if (obj != null){
            return obj.toString();
        }
        return "0";
    }
}
