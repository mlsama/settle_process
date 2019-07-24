package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardInvestReviseHis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MCardInvestReviseHisMapper {

    List<MCardInvestReviseHis> findList(@Param("date") String date,
                                        @Param("zipFileName") String zipFileName);

    CountData countData(@Param("date") String date,
                        @Param("zipFileName") String zipFileName);
}
