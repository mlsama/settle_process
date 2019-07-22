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

    List<CpuConsume> findByWhere(@Param("startNum") long startNum, @Param("endNum") long endNum);

    CountData countAmountAndNum();

    long findAllNotes();
    //统计与错误表pid,psn一致的记录数
    long findCwNotes();
    //统计错误表记录数
    long countCwNotes();


}
