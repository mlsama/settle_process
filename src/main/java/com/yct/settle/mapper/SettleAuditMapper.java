package com.yct.settle.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/22 8:54
 */
@Mapper
public interface SettleAuditMapper {

    BigDecimal countTotalAmount();
}
