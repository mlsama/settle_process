package com.yct.settle.service;

import com.yct.settle.pojo.*;

import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/12 16:14
 */
public interface ProcessResultService {

    void delAndInsert(FileProcessResult result);

    void delAndInsert(ProcessResult result);

    void delAndInsert(FileCheckError result);

    void update(FileProcessResult result);

    void delAndInsert(String date,String zipFileName,String tableName,String errorTableName);

    ExceptionRevise exceptionRevise(String date);

    List<ExceptionTrade> findPidPsnByWhere(String date, String zipFileName);

    void delByPidPsn(String tableName,String pid,String psn,String date,String zipFileName);
}
