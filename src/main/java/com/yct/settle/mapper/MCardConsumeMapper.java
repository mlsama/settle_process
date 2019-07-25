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

    List<MCardConsume> findByWhere(@Param("startNum") Long startNum,
                                   @Param("endNum") Long endNum,
                                   @Param("date") String date,
                                   @Param("zipFileName") String zipFileName);

    CountData countAmountAndNum(@Param("date") String date,
                                @Param("zipFileName") String zipFileName);

    Long findAllNotes(@Param("date") String date,
                      @Param("zipFileName") String zipFileName);

    long findCwNotes(@Param("date") String date,
                     @Param("zipFileName") String zipFileName);

    long countCwNotes(@Param("date") String date,
                      @Param("zipFileName") String zipFileName);
}
