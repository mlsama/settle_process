package com.yct.settle.mapper;

import com.yct.settle.pojo.FileCheckError;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileCheckErrorMapper {

    boolean insert(FileCheckError result);

    void del(@Param("date") String date,
             @Param("zipFileName") String zipFileName);

}
