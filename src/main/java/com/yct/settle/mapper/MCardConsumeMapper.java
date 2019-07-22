package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardConsume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 8:54
 */
@Mapper
public interface MCardConsumeMapper {

    List<MCardConsume> findByWhere(@Param("startNum") Long startNum, @Param("endNum") Long endNum);

    CountData countAmountAndNum();

    Long findAllNotes();

    long findCwNotes();

    long countCwNotes();
}
