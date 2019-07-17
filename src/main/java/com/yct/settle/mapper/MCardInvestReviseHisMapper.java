package com.yct.settle.mapper;

import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.MCardInvestReviseHis;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MCardInvestReviseHisMapper {

    List<MCardInvestReviseHis> findList();

    CountData countData();
}
