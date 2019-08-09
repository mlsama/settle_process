package com.yct.settle.mapper;

import com.yct.settle.pojo.ExceptionRevise;
import com.yct.settle.pojo.ExceptionTrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/26 17:25
 */
@Mapper
public interface ExceptionTradeMapper {

    void insert(@Param("date") String date,
                @Param("zipFileName") String zipFileName,
                @Param("tableName") String tableName,
                @Param("errorTableName") String errorTableName);

    void delByPram(@Param("date") String date,
                   @Param("zipFileName") String zipFileName);

    ExceptionRevise exceptionRevise(String date);

    List<ExceptionTrade> findPidPsnByWhere(@Param("date") String date,
                                           @Param("zipFileName") String zipFileName);

    void delByPidPsn(@Param("tableName") String tableName,
                     @Param("pid") String pid,
                     @Param("psn") String psn,
                     @Param("date") String date,
                     @Param("zipFileName") String zipFileName);
}
