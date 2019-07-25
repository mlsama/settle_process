package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.CpuConsume;
import com.yct.settle.pojo.CpuCustomerService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 8:54
 */
@Mapper
public interface CpuCustomerServiceMapper {

    List<CpuCustomerService> findList(@Param("date") String date,
                                      @Param("zipFileName") String zipFileName);

    /**
     * 统计客服数据中的充值
     * @return
     */
    CountData countInvestAmountAndNum(@Param("date") String date,
                                      @Param("zipFileName") String zipFileName);

    CountData countConsumeAmountAndNum(@Param("date") String date,
                                       @Param("zipFileName") String zipFileName);
}
