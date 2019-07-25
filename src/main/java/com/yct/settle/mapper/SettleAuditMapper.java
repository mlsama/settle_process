package com.yct.settle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/22 8:54
 */
@Mapper
public interface SettleAuditMapper {

    BigDecimal countTotalAmount(@Param("date") String date,
                                @Param("zipFileName") String zipFileName);

    BigDecimal countTotalAmountZ(@Param("date") String date,
                                 @Param("zipFileName") String zipFileName);
}
