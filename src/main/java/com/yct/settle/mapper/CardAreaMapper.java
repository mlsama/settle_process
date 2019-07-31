package com.yct.settle.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/3 8:54
 */
@Mapper
public interface CardAreaMapper {

    String getIssuesByCardNo(long cardNo);
}
