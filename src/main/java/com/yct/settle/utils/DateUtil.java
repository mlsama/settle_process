package com.yct.settle.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * DESC:日期类工具类
 * AUTHOR:mlsama
 * 2019/6/27 9:06
 */
public class DateUtil {

    /**
     * 取明天
     * @param date
     * @return
     */
    public static String getTomorrow(String date){
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(new SimpleDateFormat("yyyyMMddHHmmss").parse(date));
            calendar.add(Calendar.DAY_OF_MONTH,+1);
            Date time = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            return sdf.format(time);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
