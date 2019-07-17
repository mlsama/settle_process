package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardConsumeRevise;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 10:56
 */
@Mapper
public interface MCardConsumeReviseMapper {

    List<MCardConsumeRevise> findList();

    CountData countAmountAndNum();

}
