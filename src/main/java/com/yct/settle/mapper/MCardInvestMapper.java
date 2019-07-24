package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardInvest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MCardInvestMapper {

    List<MCardInvest> findByWhere(@Param("startNum") long startNum,
                                  @Param("endNum") long endNum,
                                  @Param("date") String date,
                                  @Param("zipFileName") String zipFileName);

    CountData countData(@Param("date") String date,
                        @Param("zipFileName") String zipFileName);

    long findAllNotes(@Param("date")String date,
                      @Param("zipFileName")String outZipFileName);

    CountData countAllData(@Param("date")String date,
                           @Param("zipFileName")String outZipFileName);
}
