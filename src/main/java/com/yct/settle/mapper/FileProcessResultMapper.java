package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.FileProcessResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileProcessResultMapper {

    boolean insert(FileProcessResult result);

    boolean insertWithoutNull(FileProcessResult result);

    boolean update(FileProcessResult result);

    CountData countInvestDate(String date);

    CountData countConsumeDate(String date);

    CountData countReviseDate(String date);

    CountData countCpuDate(String date);

    CountData countMCardDate(String date);

    void delByDate(String date);

    CountData countCustomerDate(String date);

    CountData countCpuCustomerDate(String date);

    CountData countMCardCustomerDate(String date);

    void del(@Param("date") String date, @Param("zipFileName")String zipFileName);
}
