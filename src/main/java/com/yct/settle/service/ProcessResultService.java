package com.yct.settle.service;

import com.yct.settle.pojo.FileProcessResult;
import com.yct.settle.pojo.ProcessResult;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/12 16:14
 */
public interface ProcessResultService {

    void delAndInsert(FileProcessResult result);

    void delAndInsert(ProcessResult result);

    void update(FileProcessResult result);

    void delAndInsert(String date,String zipFileName,String tableName,String errorTableName);
}
