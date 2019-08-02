package com.yct.settle.utils;

import java.math.BigDecimal;

public class AmountUtil {

    /**
     *  转为指定位数的字符串
     * @param v
     * @param len
     * @return
     */
    public static String convertToString(BigDecimal v, int len){
        if (v == null){
            v = new BigDecimal("0");
        }
        BigDecimal bigDecimal = v.setScale(2, BigDecimal.ROUND_HALF_DOWN);
        String result = bigDecimal.toString();
        while (true){
            if (result.length() < len){
                result = "0"+result;
            }else {
                break;
            }
        }
        return result;
    }

    /**
     * 加法
     * @param v1
     * @param v2
     * @return
     */
    public static BigDecimal add(BigDecimal v1, BigDecimal v2){
        if (v1 == null){
            v1 = new BigDecimal("0");
        }
        if (v2 == null){
            v2 = new BigDecimal("0");
        }
        return v1.add(v2).setScale(2,BigDecimal.ROUND_HALF_UP);
    }
    /**
     * 加法
     * @param v
     * @return
     */
    public static BigDecimal adds(BigDecimal ... v){
        BigDecimal result = new BigDecimal("0");
        for(int i = 0; i < v.length; i++){
            if (v[i] == null){
                v[i] = new BigDecimal("0");
            }
            result = result.add(v[i]);
        }
        return result;
    }
}
