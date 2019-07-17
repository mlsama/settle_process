package com.yct.settle.thread;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * DESC:线程任务处理类
 * AUTHOR:mlsama
 * 2019/7/8 15:36
 */
@Service
public class ThreadTaskHandle {
    private Boolean isError;

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void setIsError(Boolean isError){
        this.isError = isError;
    }
    public Boolean getIsError(){
       return isError;
    }

    public void handle(Runnable task){
        threadPoolTaskExecutor.execute(task);
    }
}
