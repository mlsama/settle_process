package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.CpuTrade;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/6/27 9:38
 */
@Mapper
public interface CpuTradeMapper {

    void batchInsert(List<CpuTrade> list);

    List<CpuTrade> findList();

    CountData findCpuInvestSum();

}
