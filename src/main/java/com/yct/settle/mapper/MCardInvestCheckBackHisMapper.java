package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardInvestCheckBackHis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 10:56
 */
@Mapper
public interface MCardInvestCheckBackHisMapper {

    List<MCardInvestCheckBackHis> findList(@Param("date") String date,
                                           @Param("zipFileName") String zipFileName);

    CountData countData(@Param("date") String date,
                        @Param("zipFileName") String zipFileName);
}
