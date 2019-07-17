package com.yct.settle.mapper;

import com.yct.settle.pojo.ProcessResult;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessResultMapper {

    boolean insert(ProcessResult result);

    void del(String settleDate);
}
