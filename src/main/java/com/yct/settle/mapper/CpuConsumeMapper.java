package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.CpuConsume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 8:54
 */
@Mapper
public interface CpuConsumeMapper {

    List<CpuConsume> findByWhere(@Param("startNum") long startNum,
                                 @Param("endNum") long endNum,
                                 @Param("date") String date,
                                 @Param("zipFileName") String zipFileName);

    CountData countAmountAndNum(@Param("date") String date,
                                @Param("zipFileName") String zipFileName);

    long findAllNotes(@Param("date") String date,
                      @Param("zipFileName") String zipFileName);

    //统计与错误表pid,psn一致的记录数
    long findCwNotes(@Param("date") String date,
                     @Param("zipFileName") String zipFileName);

    //统计错误表记录数
    long countCwNotes(@Param("date") String date,
                      @Param("zipFileName") String zipFileName);


}
