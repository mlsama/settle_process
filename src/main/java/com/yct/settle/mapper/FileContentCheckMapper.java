package com.yct.settle.mapper;

import com.yct.settle.pojo.FileContentCheck;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileContentCheckMapper {

    boolean insert(FileContentCheck result);

}
