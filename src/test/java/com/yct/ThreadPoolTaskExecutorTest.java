package com.yct;

import com.yct.settle.thread.ThreadTaskHandle;
import com.yct.settle.utils.FileUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

/**
 * DESC: 线程顺序执行测试
 * AUTHOR:mlsama
 * 2019/7/8 15:06
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {com.yct.settle.Application.class})
public class ThreadPoolTaskExecutorTest {
    private final Logger log = LoggerFactory.getLogger(ThreadPoolTaskExecutorTest.class);
    File resultFile = new File("E:\\result\\DM20190618\\result.txt");
    @Resource
    private ThreadTaskHandle threadTaskHandle;
    private int threadNum = 3;
    @Test
    public void testThreads() {
        try {
            Map<String,String> map = new HashMap<>();
            CyclicBarrier barrier=new CyclicBarrier(threadNum);
            log.info("开始线程测试！");
            threadTaskHandle.handle(() ->{
                File file = new File("E:\\result\\DM20190618\\CJ20190618.txt");
               doSomething(map, file, "thread1");
                try {
                    //到达屏障
                    barrier.await();
                } catch (Exception e) {
                    log.error("线程到达同步点1，进行线程阻塞发生异常：{}",e);
                    // 中断当前线程
                    Thread.currentThread().interrupt();
                }
            });

            threadTaskHandle.handle(() -> {
                File file = new File("E:\\result\\DM20190618\\MJ20190618.txt");
               doSomething(map,file,"thread2");
                try {
                    //到达屏障
                    barrier.await();
                } catch (Exception e) {
                    log.error("线程到达同步点2，进行线程阻塞发生异常：{}",e);
                    // 中断当前线程
                    Thread.currentThread().interrupt();
                }
            });

            //主线程
            try {
                //阻塞当前线程直到latch中数值为零才执行
                barrier.await();
            } catch (Exception e) {
                log.error("线程到达同步点3，进行线程阻塞发生异常：{}",e);
                // 中断当前线程
                Thread.currentThread().interrupt();
            }
            log.info("主线程执行！");
            if (map.get("thread1")=="0000" && map.get("thread2")=="0000"){
                log.info("处理成功！");
            }else {
                log.error("处理失败！");
            }
        }catch (Exception e){
            log.error("***********发生异常，处理失败！***********");
        }
    }

    private synchronized void doSomething(Map<String,String> map,File resource,String key){
        log.info("开始写入文件，当前线程是{}",key);
        if (threadTaskHandle.getIsError()){
            log.info("有线程发生了异常，本线程无需再执行！");
            return ;
        }
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        try {
            int i = 1/0;
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(resource)));
            //写入
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile),"UTF-8"),16);
            String line;
            while ((line = bufferedReader.readLine()) != null){
                bufferedWriter.write(key+"\t"+line+System.getProperty("line.separator"));
            }
            log.info("写入文件成功");
        }catch (Exception e) {
            threadTaskHandle.setIsError(true);
            log.error("写入文件发生异常,修改标志，通知其他线程");
            return ;
        } finally {
            FileUtil.closeWriter(bufferedWriter);
            FileUtil.closeReader(bufferedReader);
        }
        map.put(key,"0000");
        return ;
    }

}
