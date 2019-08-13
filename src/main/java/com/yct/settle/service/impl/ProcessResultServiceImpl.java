package com.yct.settle.service.impl;

import com.yct.settle.mapper.ExceptionTradeMapper;
import com.yct.settle.mapper.FileCheckErrorMapper;
import com.yct.settle.mapper.FileProcessResultMapper;
import com.yct.settle.mapper.ProcessResultMapper;
import com.yct.settle.pojo.*;
import com.yct.settle.service.ProcessResultService;
import com.yct.settle.utils.AmountUtil;
import com.yct.settle.utils.MathUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/12 16:16
 */
@Service
public class ProcessResultServiceImpl implements ProcessResultService {
    @Resource
    private FileProcessResultMapper fileProcessResultMapper;
    @Resource
    private ProcessResultMapper processResultMapper;
    @Resource
    private ExceptionTradeMapper exceptionTradeMapper;
    @Resource
    private FileCheckErrorMapper fileCheckErrorMapper;

    @Override
    public void delAndInsert(FileProcessResult result) {
        fileProcessResultMapper.del(result.getSettleDate(),result.getZipFileName());
        fileProcessResultMapper.insertWithoutNull(result);

    }

    @Override
    public void delAndInsert(ProcessResult result) {
        processResultMapper.del(result.getSettleDate());
        processResultMapper.insert(result);
    }

    @Override
    public void delAndInsert(FileCheckError result) {
        //fileCheckErrorMapper.del(result.getSettleDate(),result.getZipFileName());
        fileCheckErrorMapper.insert(result);
    }

    @Override
    public void update(FileProcessResult result) {
        fileProcessResultMapper.update(result);
    }

    @Override
    public void delAndInsert(String date, String zipFileName,String tableName,String errorTableName) {
        exceptionTradeMapper.delByPram(date,zipFileName);
        exceptionTradeMapper.insert(date,zipFileName,tableName,errorTableName);
    }

    @Override
    public ExceptionRevise exceptionRevise(String date) {
        ExceptionRevise exceptionRevise = exceptionTradeMapper.exceptionRevise(date);
        if (exceptionRevise != null){
            long notes = MathUtil.longMinus(exceptionRevise.getExceptionNotes(),exceptionRevise.getErrorNotes());
            BigDecimal amount = AmountUtil.minus(exceptionRevise.getExceptionAmount(), exceptionRevise.getErrorAmount());
            exceptionRevise.setNotes(notes);
            exceptionRevise.setAmount(amount);
        }
        return exceptionRevise;
    }

    @Override
    public List<ExceptionTrade> findPidPsnByWhere(String date, String zipFileName) {
        return exceptionTradeMapper.findPidPsnByWhere(date,zipFileName);
    }

    @Override
    public void delByPidPsn(String tableName, String pid, String psn, String date, String zipFileName) {
        exceptionTradeMapper.delByPidPsn(tableName,pid,psn,date,zipFileName);
    }

}
