package com.yct.settle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/8 17:30
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor createThreadPool(){
        ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();
        //核心线程数
        threadPool.setCorePoolSize(2);
        //最大线程数
        threadPool.setMaxPoolSize(10);
        //线程最大空闲时间
        threadPool.setKeepAliveSeconds(3);
        //队列大小
        threadPool.setQueueCapacity(10);
        //线程池工厂
        threadPool.setThreadFactory(new ThreadPoolExecutorFactoryBean());
        //线程名称前缀
        //threadPool.setThreadNamePrefix("settle-process");
        threadPool.setBeanName("settle-process");
        //拒绝策略
        threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        //初始化线程池,不然报错
        threadPool.initialize();
        return threadPool;
    }
}
