package com.yct.settle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 8:54
 */
@Mapper
public interface MCardConsumeErrorNoBusMapper {

    long countCwNotes(@Param("date") String date,
                     @Param("zipFileName") String zipFileName);
}
