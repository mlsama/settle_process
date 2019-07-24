package com.yct.settle.mapper;

import com.yct.settle.pojo.FileCheckError;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileCheckErrorMapper {

    boolean insert(FileCheckError result);

}
