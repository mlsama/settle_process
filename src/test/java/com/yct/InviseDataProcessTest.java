package com.yct;

import com.yct.settle.mapper.CpuTradeMapper;
import com.yct.settle.pojo.CpuTrade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/6/25 18:13
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = {com.yct.settle.Application.class})
public class InviseDataProcessTest {
    @Resource
    private CpuTradeMapper cpuTradeMapper;
    @Test
    public void batchInsertTest(){
        ArrayList<CpuTrade> cpuTrades = new ArrayList<>();
        CpuTrade cpuTrade = new CpuTrade();
        cpuTrade.setQDATE("20190618");
        cpuTrade.setQNAME("CC2110000320190617.ZIP");
        cpuTrade.setDT("01");//充值
        cpuTrade.setTT("02");//充值
        cpuTrade.setDMON("0000000000000");//扩展信息
        cpuTrade.setISSUEA("00");
        cpuTrade.setUSEA("00");
        cpuTrade.setPID("123456");
        cpuTrade.setPSN("123456");
        cpuTrade.setTIM("123456");
        cpuTrade.setLCN("123456");
        cpuTrade.setFCN("123456");
        cpuTrade.setTF(new BigDecimal("12345.00"));
        cpuTrade.setFEE(new BigDecimal("12345.00"));
        cpuTrade.setBAL(new BigDecimal("12345.00"));
        cpuTrade.setATT("01");
        cpuTrade.setCRN("1");
        cpuTrade.setXRN("566");
        cpuTrade.setEPID("123456");
        cpuTrade.setETIM("123456");
        cpuTrade.setLPID("123456");
        cpuTrade.setLTIM("123456");
        cpuTrade.setTAC("123456");
        cpuTrade.setFLAG("1");
        cpuTrades.add(cpuTrade);
        CpuTrade cpuTrade1 = new CpuTrade();
        cpuTrade1.setQDATE("20190618");
        cpuTrade1.setQNAME("CC2110000320190617.ZIP");
        cpuTrade1.setDT("01");//充值
        cpuTrade1.setTT("02");//充值
        cpuTrade1.setDMON("0000000000000");//扩展信息
        cpuTrade1.setISSUEA("00");
        cpuTrade1.setUSEA("00");
        cpuTrade1.setPID("123456");
        cpuTrade1.setPSN("123456");
        cpuTrade1.setTIM("123456");
        cpuTrade1.setLCN("123456");
        cpuTrade1.setFCN("123456");
        cpuTrade1.setTF(new BigDecimal("12345.00"));
        cpuTrade1.setFEE(new BigDecimal("12345.00"));
        cpuTrade1.setBAL(new BigDecimal("12345.00"));
        cpuTrade1.setATT("01");
        cpuTrade1.setCRN("1");
        cpuTrade1.setXRN("566");
        cpuTrade1.setEPID("123456");
        cpuTrade1.setETIM("123456");
        cpuTrade1.setLPID("123456");
        cpuTrade1.setLTIM("123456");
        cpuTrade1.setTAC("123456");
        cpuTrade1.setFLAG("1");
        cpuTrades.add(cpuTrade1);
        cpuTradeMapper.batchInsert(cpuTrades);
    }

    @Test
    public void getFilesTest(){
        File[] files = new File("E://testData//input").listFiles();
        System.out.println(files);
    }

    public void lenTest(){
        String s = "20190618\t20190618\t01\t0\t01\t01\t880120012655\t0000113719\t20190617172644\t5100008536677698\tF1BF011B0E40FE21\t00050.00\t00050.00\t00050.60\t02\t14\t00016\t00528\t0000000000000\t880120012655\t20190617172644\t010000200764\t06171437\t587D0DBA\t20000000000000000000\tCC2110000320190617.ZIP";
        String ss = "20190618\t20190618\t01\t0\t01\t01\t880120012655\t0000113718\t20190617171937\t5100001007595680\tA171471D5E8EB821\t00100.00\t00100.00\t00140.00\t02\t14\t00006\t00210\t0000000000000\t880120012655\t20190617171937\t010000182397\t06170701\t54B79768\t10000000000000000000\tCC2110000320190617.ZIP";
    }

    @Test
    public void longTest(){
        Map<String, Object> countMap = new HashMap<>();
        if (countMap.get("ml") != null){
            Long tem = (Long)countMap.get("ml");
            countMap.put("ml",tem + 1L);
        }else {
            countMap.put("ml",1L);
        }

        System.out.println(countMap);
    }

}
