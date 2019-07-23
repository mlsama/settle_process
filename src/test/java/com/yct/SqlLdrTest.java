package com.yct;

import com.yct.settle.utils.SqlLdrUtil;
import com.yct.sqlldr.SqlLdr2;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/1 16:03
 */
public class SqlLdrTest {
    //日记
    private final Logger log = LoggerFactory.getLogger(SqlLdrTest.class);
    @Test
    public void cpuInvestInsertTest(){
        //表名
        String tableName = null;
        //表字段
        String fieldNames = null;
        //控制文件
        File contlFile = null;
        tableName = "T_CPU_INVEST";
        fieldNames = "(PID, PSN, TIM, " +
                "      LCN, FCN, TF, FEE, " +
                "      BAL, TT, ATT, CRN, " +
                "      XRN, DMON, BDCT, MDCT, " +
                "      UDCT, EPID, ETIM, LPID, " +
                "      LTIM, AREA, ACT, SAREA, " +
                "      TAC, APP, FLAG, ERRNO)";
        //控制文件
        contlFile = new File("E:\\sqlldrTest\\cpuInvest.ctl");
        File dataFile = new File("E:\\yct\\settleData\\output\\20121226\\CC2052000320121225\\JY2052000320121225.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                                    tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    @Test
    public void cpuInvestCheckInsertTest(){
        String tableName = "T_CPU_INVEST_CHECKBACK";
        String fieldNames =  "(PID, PSN, TIM, " +
                "      LCN, FCN, TF, FEE, " +
                "      BAL, TT, ATT, CRN, " +
                "      XRN, LPID, LTIM, APP, " +
                "      FLAG, ERRNO)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\cpuInvestCheckBack.ctl");
        File dataFile = new File("E:\\yct\\settleData\\output\\20121226\\CC2052000320121225\\CZ2052000320121225.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }


    @Test
    public void mCardInvestInsertTest(){
        //表名
        String tableName = null;
        //表字段
        String fieldNames = null;
        //控制文件
        File contlFile = null;
        tableName = "T_MCARD_INVEST";
        fieldNames = "(PSN, LCN, FCN, " +
                "      LPID, LTIM, PID, TIM, " +
                "      TF, BAL, TT, RN, " +
                "      EPID, ETIM, AI, VC, " +
                "      TAC, APP, FLAG, ERRNO)";
        //控制文件
        contlFile = new File("E:\\sqlldrTest\\mCardInvest.ctl");
        File dataFile = new File("E:\\testData\\input\\20121224\\CZ2031000320121223\\JY2031000320121223.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                                    tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }
    @Test
    public void mCardInvestCheckInsertTest(){
        String tableName = "T_MCARD_INVEST_CHECKBACK";
        String fieldNames = "(PSN, LCN, FCN, " +
                "      LPID, LTIM, PID, TIM, " +
                "      TF, BAL, TT, RN, " +
                "      APP, FLAG, ERRNO)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardInvestCheckBack.ctl");
        File dataFile = new File("E:\\testData\\input\\20121224\\CZ2031000320121223\\CZ2031000320121223.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                                    tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    @Test
    public void mCardInvestReviseHisInsertTest(){
        String tableName = "T_MCARD_INVEST_REVISE_HIS";
        String fieldNames = "(PSN, LCN, FCN, " +
                "      LPID, LTIM, PID, TIM, " +
                "      TF, BAL, TT, RN, " +
                "      EPID, ETIM, AI, VC, " +
                "      TAC, APP, FLAG, ERRNO)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardInvestReviseHis.ctl");
        File dataFile = new File("E:\\yct\\settleData\\output\\20110309\\CZ2030000120110308\\XZ2030000120110308.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                                    tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    @Test
    public void mCardInvestCheckHisInsertTest(){
        String tableName = "T_MCARD_INVEST_CHECKBACK_HIS";
        String fieldNames = "(PSN, LCN, FCN, " +
                "      LPID, LTIM, PID, TIM, " +
                "      TF, BAL, TT, RN, " +
                "      APP, FLAG, ERRNO)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardInvestCheckBackHis.ctl");
        File dataFile = new File("E:\\yct\\settleData\\output\\20110309\\CZ2039000120110308\\LC2039000120110308.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                                    tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    /**------------------------消费------------------------------*/

    @Test
    public void cpuConsumeTest(){
        String tableName = "T_CPU_CONSUME";
        String fieldNames = "(PID,PSN,TIM,LCN,FCN,TF,FEE,BAL,TT,ATT,CRN,XRN,DMON,BDCT,MDCT,UDCT,EPID,ETIM,LPID,LTIM,AREA,ACT,SAREA,TAC,MEM)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\cpuConsume.ctl");
        File dataFile = new File("E:\\yct\\settleData\\input\\20121224\\CX802300012012122114\\JY802300012012122114.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }
    @Test
    public void cpuConsumeNoBusTest(){
        String tableName = "T_CPU_CONSUME_NOBUS";
        String fieldNames =  "(PID, PSN, TIM, LCN, FCN, TF, FEE, " +
                                "BAL, TT, ATT, CRN, XRN, DMON, " +
                                "EPID, ETIM, LPID, LTIM, TAC)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\cpuConsumeNoBus.ctl");
        File dataFile = new File("E:\\yct\\settleData\\input\\20121224\\CX9010000120121224\\JY9010000120121224.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }
    @Test
    public void cpuConsumeErrorNoBusTest(){
        String tableName = "T_CPU_CONSUME_ERROR_NOBUS";
        String fieldNames =  "(PID, PSN, TAC, STATUS)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\cpuConsumeErrorNoBus.ctl");
        File dataFile = new File("E:\\yct\\settleData\\output\\20121227\\CX9010000120121227\\CW9010000120121227.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    @Test
    public void mCardConsumeTest(){
        String tableName = "T_MCARD_CONSUME";
        String fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, FEE, TT, RN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, AI, VC, TAC)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardConsume.ctl");
        File dataFile = new File("E:\\yct\\settleData\\input\\20121226\\XF268800120121225\\JY268800120121225.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    @Test
    public void mCardConsumeErrorTest(){
        String date = "20190723";
        String tableName = "T_MCARD_CONSUME_ERROR";
        String fieldNames = "(PSN constant "+date+",PID, STATUS)";
        //控制文件
        File contlFile = new File("E:\\insertTest.ctl");
        File dataFile = new File("E:\\insertTest.txt");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }
    @Test
    public void mCardConsumeReviseTest(){
        String tableName = "T_MCARD_CONSUME_REVISE";
        String fieldNames = "(FNAME, PSN, LCN, FCN, LPID, LTIM, PID, TIM, " +
                                "TF, BAL, FEE, TT, RN, DMON, BDCT, MDCT, " +
                                "UDCT, EPID, ETIM, AI, VC, TAC, FLAG, CODE)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardConsumeRevise.ctl");
        File dataFile = new File("E:\\yct\\settleData\\output\\20121226\\XF268800120121225\\CW001800120121225.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }
    @Test
    public void mCardConsumeNoBusTest(){
        String tableName = "T_MCARD_CONSUME_NOBUS";
        String fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, TT, RN, EPID, ETIM, AI, VC, TAC, MEM)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardConsumeNoBus.ctl");
        File dataFile = new File("E:\\yct\\settleData\\input\\20110309\\XF200430120110308\\JY200430120110308.txt");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }
    @Test
    public void mCardConsumeErrorNoBusTest(){
        String tableName = "T_MCARD_CONSUME_ERROR_NOBUS";
        String fieldNames = "(PSN, PID, STATUS)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardConsumeErrorNoBus.ctl");
        File dataFile = new File("E:\\yct\\settleData\\output\\20110309\\XF3901000120110309\\CW3901000120110309.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    /*****---------------------客服-------------------------------*****/

    @Test
    public void mCardCustomerServiceTest(){
        String tableName = "T_MCARD_CUSTOMER_SERVICE";
        String fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, TT, RN, EPID, ETIM, AI, VC, TAC)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\mCardCustomerService.ctl");
        File dataFile = new File("E:\\yct\\settleData\\input\\20121225\\KF2075000120121224\\JY2075000120121224.TXT");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }
    @Test
    public void cpuCustomerServiceTest(){
        String tableName = "T_CPU_CUSTOMER_SERVICE";
        String fieldNames = "(PID,PSN,TIM,LCN,FCN,TF,FEE,BAL,TT,ATT,CRN,XRN,DMON,BDCT,MDCT,UDCT,EPID,ETIM,LPID,LTIM,AREA,ACT,SAREA,TAC)";
        //控制文件
        File contlFile = new File("E:\\sqlldrTest\\cpuCustomerService.ctl");
        File dataFile = new File("");
        boolean b = SqlLdrUtil.insertBySqlLdr("SCOTT", "ml", "orcl",
                tableName, fieldNames, contlFile, dataFile);
        if (b){
            log.info("***********导入成功**********");
        }else {
            log.error("***********导入失败**********");
        }
    }

    @Test
    public void ctlTest(){
        SqlLdr2.Execute();
    }

}
