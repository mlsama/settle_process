package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.CpuConsumeRevise;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 10:56
 */
@Mapper
public interface CpuConsumeReviseMapper {

    List<CpuConsumeRevise> findList();

    CountData countAmountAndNum();
}
