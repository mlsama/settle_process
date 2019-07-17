package com.yct.settle.mapper;

import com.yct.settle.pojo.CpuTradeRevise;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/6/27 9:38
 */
@Mapper
public interface CpuTradeReviseMapper {

    void batchInsert(List<CpuTradeRevise> list);

    List<CpuTradeRevise> findList();

    Long findCpuInvestReviseSum();
}
