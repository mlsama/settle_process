package com.yct.settle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/26 17:25
 */
@Mapper
public interface ExceptionTradeMapper {

    void insert(@Param("date") String date,
                @Param("zipFileName") String zipFileName);

    void delByPram(@Param("date") String date,
                   @Param("zipFileName") String zipFileName);
}
