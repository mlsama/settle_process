package com.yct;

import com.yct.settle.manager.DataProcessManager;
import com.yct.settle.mapper.CpuConsumeMapper;
import com.yct.settle.mapper.MCardConsumeMapper;
import com.yct.settle.mapper.MCardConsumeReviseMapper;
import com.yct.settle.pojo.CpuConsume;
import com.yct.settle.pojo.MCardConsume;
import com.yct.settle.pojo.MCardConsumeRevise;
import com.yct.settle.utils.AmountUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/6/25 18:13
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {com.yct.settle.Application.class})
public class DataProcessTest {
    private final Logger log = LoggerFactory.getLogger(DataProcessTest.class);
    @Resource
    private DataProcessManager dataProcessManager;
    @Resource
    private CpuConsumeMapper cpuConsumeMapper;
    @Resource
    private MCardConsumeMapper mCardConsumeMapper;
    @Resource
    private MCardConsumeReviseMapper mCardConsumeReviseMapper;
    @Test
    public void ccProcessTest(){
        dataProcessManager.settleDataProcess();
    }

    @Test
    public void getFilesTest(){
        File[] files = new File("E://testData//input").listFiles();
        System.out.println(files);
    }
    @Test
    public void convertStringTest(){
        //获取总笔数
        long allNotes = mCardConsumeMapper.findAllNotes();
        if (allNotes > 0L){
            long pageSize = 1000000L,startNum = 0L,count = 1L,endNum;
            while (allNotes > 0L){
                if (allNotes <= pageSize){
                    pageSize = allNotes;
                }
                endNum = startNum + pageSize;
                log.info("第{}次，allNotes={},pageSize={},startNum={},endNum={}",count,allNotes,pageSize,startNum,endNum);
                allNotes -= pageSize;
                startNum += pageSize;
                count++;
            }
            log.info("*************结束了！*************");
        }
    }

    @Test
    public void cpuConsumeMapperTest(){
        List<CpuConsume> byWhere = cpuConsumeMapper.findByWhere(0, 100);
        System.out.println(byWhere);
    }

    @Test
    public void mCardConsumeMapperTest(){
        List<MCardConsume> byWhere = mCardConsumeMapper.findByWhere(0L, 10L);
        System.out.println(byWhere.size());
    }

    @Test
    public void addsTest(){
        BigDecimal result = AmountUtil.adds(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4"));
        System.out.println(result);
    }
    @Test
    public void dmCxSumTest(){
        //BigDecimal add = AmountUtil.add(new BigDecimal("0"), new BigDecimal("1"));
        System.out.println(1/10);
    }
    @Test
    public void mCardConsumeReviseTest(){
        List<MCardConsumeRevise> reviseList = mCardConsumeReviseMapper.findList();
        for (MCardConsumeRevise mCardConsumeRevise : reviseList){
            System.out.println(1);
        }
    }

}
