package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardCustomerService;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 8:54
 */
@Mapper
public interface MCardCustomerServiceMapper {

    List<MCardCustomerService> findList();

    CountData countInvestAmountAndNum();

    CountData countConsumeAmountAndNum();
}
