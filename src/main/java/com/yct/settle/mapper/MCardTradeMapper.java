package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardTrade;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/6/27 9:38
 */
@Mapper
public interface MCardTradeMapper {

    void batchInsert(List<MCardTrade> list);

    List<MCardTrade> findList();

    CountData findMcardInvestSum();
}
