package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.CustomerService;
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

    CountData countCpuInvestReviseDate(String date);

    CountData countCpuConsumerReviseDate(String date);

    CountData countMCardInvestReviseDate(String date);

    CountData countMCardConsumerReviseDate(String date);

    CountData countCpuDate(String date);

    CountData countMCardDate(String date);

    void delByDate(String date);

    CustomerService countCpuCustomerDate(String date);

    CustomerService countMCardCustomerDate(String date);

    void del(@Param("date") String date, @Param("zipFileName")String zipFileName);
}
