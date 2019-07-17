package com.yct.settle.mapper;

import com.yct.settle.pojo.MCardTradeRevise;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/6/27 9:38
 */
@Mapper
public interface MCardTradeReviseMapper {

    void batchInsert(List<MCardTradeRevise> list);

    List<MCardTradeRevise> findList();

    Long findCpuInvestReviseSum();
}
