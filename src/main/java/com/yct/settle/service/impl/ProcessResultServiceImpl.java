package com.yct.settle.service.impl;

import com.yct.settle.mapper.FileProcessResultMapper;
import com.yct.settle.mapper.ProcessResultMapper;
import com.yct.settle.pojo.FileProcessResult;
import com.yct.settle.pojo.ProcessResult;
import com.yct.settle.service.ProcessResultService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
    public void update(FileProcessResult result) {
        fileProcessResultMapper.update(result);
    }
}
