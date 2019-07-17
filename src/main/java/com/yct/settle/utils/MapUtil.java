package com.yct.settle.utils;

import java.util.Map;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/5 11:29
 */
public class MapUtil {

    public static void mapDataAdd(Map<String,Object> countMap,String key,Long value) {
        if (value != 0L) {
            if (countMap.get(key) != null) {
                value += (Long) countMap.get(key);
            }
            countMap.put(key, value);
        }
    }
}
