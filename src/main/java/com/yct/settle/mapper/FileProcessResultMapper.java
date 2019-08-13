package com.yct.settle.mapper;

import com.yct.settle.pojo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileProcessResultMapper {

    boolean insert(FileProcessResult result);

    boolean insertWithoutNull(FileProcessResult result);

    boolean update(FileProcessResult result);

    CpuInvestData countCpuInvestDate(String date);

    MCardInvestData countMCardInvestDate(String date);

    CpuConsumerData countCpuConsumerDate(String date);

    MCardConsumerData countMCardConsumerDate(String date);

    void delByDate(String date);

    CustomerService countCpuCustomerDate(String date);

    CustomerService countMCardCustomerDate(String date);

    void del(@Param("date") String date,
             @Param("zipFileName")String zipFileName);
}
