package com.yct.settle.service.impl;

import com.yct.settle.mapper.*;
import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.FileCheckError;
import com.yct.settle.pojo.FileProcessResult;
import com.yct.settle.service.AreaService;
import com.yct.settle.service.BatchInsertService;
import com.yct.settle.thread.ThreadTaskHandle;
import com.yct.settle.utils.FileUtil;
import com.yct.settle.utils.SqlLdrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/8 11:05
 */
@Slf4j
@Service
public class BatchInsertServiceImpl implements BatchInsertService {
    @Resource
    private ProcessResultServiceImpl processResultService;
    @Resource
    private CpuInvestMapper cpuInvestMapper;
    @Resource
    private MCardInvestMapper mCardInvestMapper;
    /**
     * sqlldr批量导入
     * @param date              清算日期
     * @param unZipFile         落库的文件
     * @param outZipFileName    落库的文件所在的压缩文件
     * @param dbUser
     * @param dbPassword
     * @param sqlldrDir
     * @param resultMap
     * @return
     */
    @Override
    public boolean batchInsertInvestData(String date, File unZipFile, String outZipFileName, String dbUser,
                                                String dbPassword, String odbName, File sqlldrDir, File targetFile,Map<String, String> resultMap) {
        //表名
        String tableName = null;
        //表字段
        String fieldNames = null;
        //控制文件
        File contlFile = null;
        String fileName = unZipFile.getName();
        if (outZipFileName.startsWith("CC")) {  //cpu卡充值
            if (fileName.startsWith("JY")){
                log.info("开始对cpu卡充值文化进行内容校验");
                tableName = "T_CPU_INVEST";
                fieldNames = "(" +
                        "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                        "      PID, PSN, TIM, " +
                        "      LCN, FCN, TF, FEE, " +
                        "      BAL, TT, ATT, CRN, " +
                        "      XRN, DMON, BDCT, MDCT, " +
                        "      UDCT, EPID, ETIM, LPID, " +
                        "      LTIM, AREA, ACT, SAREA, " +
                        "      TAC, APP, FLAG, ERRNO)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuInvest.ctl");
                boolean inputFileFlag = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,
                        fieldNames, contlFile,targetFile);
                if (!inputFileFlag){
                    //修改
                    processResultService.update(
                            new FileProcessResult(outZipFileName, targetFile.getAbsolutePath(), new Date(),
                                    "6555", "落库失败"));
                    return false;
                }
                //汇总
                CountData countData = cpuInvestMapper.countAllData(date,outZipFileName);
                long inputInvestNotes = 0L;
                BigDecimal inputInvestAmount = new BigDecimal("0");
                if (countData != null){
                    inputInvestNotes = countData.getNotesSum();
                    inputInvestAmount = countData.getAmountSum();
                }
                boolean outputFileFlag = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,
                        fieldNames,contlFile,unZipFile);
                if (!outputFileFlag){
                    //修改
                    processResultService.update(
                            new FileProcessResult(outZipFileName, unZipFile.getAbsolutePath(), new Date(),
                                    "6555", "落库失败"));
                    return false;
                }
                //汇总
                CountData oCountData = cpuInvestMapper.countAllData(date, outZipFileName);
                long outputInvestNotes = 0L;
                BigDecimal outputInvestAmount = new BigDecimal("0");
                if (countData != null){
                    outputInvestNotes = oCountData.getNotesSum();
                    outputInvestAmount = oCountData.getAmountSum();
                }
                if (inputInvestNotes != outputInvestNotes || inputInvestAmount.compareTo(outputInvestAmount) != 0){
                    log.error("input文件{}的笔数或者金额不等于output对应的文件。input笔数：{}，金额:{}。output笔数：{}，金额:{}",
                            unZipFile.getAbsolutePath(),inputInvestNotes,inputInvestAmount,outputInvestNotes,outputInvestAmount);
                    //插入文件检查表
                    processResultService.delAndInsert(new FileCheckError(date,unZipFile.getAbsolutePath(),"01","01",new Date(),
                            "6555","input文件的笔数或者金额不等于output对应的文件",inputInvestNotes,
                            inputInvestAmount,outputInvestNotes,outputInvestAmount,0L,0L,
                            new BigDecimal("0"),new BigDecimal("0")));
                    //修改
                    processResultService.update(
                            new FileProcessResult(outZipFileName, unZipFile.getAbsolutePath(), new Date(),
                                    "6555", "文件交易笔数，金额校验失败"));
                }else {
                    log.info("{}校验成功。",outZipFileName);
                }
                return true;
            }else if (fileName.startsWith("CZ")){
                tableName = "T_CPU_INVEST_CHECKBACK";
                fieldNames = "(" +
                        "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                        "      PID, PSN, TIM, " +
                        "      LCN, FCN, TF, FEE, " +
                        "      BAL, TT, ATT, CRN, " +
                        "      XRN, LPID, LTIM, APP, " +
                        "      FLAG, ERRNO)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuInvestCheckBack.ctl");
            }else if (fileName.startsWith("XZ")){
                tableName = "T_CPU_INVEST_REVISE_HIS";
                fieldNames = "(" +
                        "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                        "      PID, PSN, TIM, " +
                        "      LCN, FCN, TF, FEE, " +
                        "      BAL, TT, ATT, CRN, " +
                        "      XRN, DMON, BDCT, MDCT, " +
                        "      UDCT, EPID, ETIM, LPID, " +
                        "      LTIM, AREA, ACT, SAREA, " +
                        "      TAC, APP, FLAG, ERRNO)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuInvestReviseHis.ctl");
            }else if (fileName.startsWith("LC")){
                tableName = "T_CPU_INVEST_CHECKBACK_HIS";
                fieldNames = "(" +
                        "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                        "      PID, PSN, TIM, " +
                        "      LCN, FCN, TF, FEE, " +
                        "      BAL, TT, ATT, CRN, " +
                        "      XRN, LPID, LTIM, APP, " +
                        "      FLAG, ERRNO)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuInvestCheckBackHis.ctl");
            }
        }else { //m1卡充值
            //是否的特殊的文件
            boolean isNewCw = FileUtil.isNewCz(targetFile);
            if (fileName.startsWith("JY")){
                log.info("开始对m1卡充值文化进行内容校验");
                tableName = "T_MCARD_INVEST";
                if (isNewCw){
                    fieldNames = "(" +
                            "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                            "      PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      EPID, ETIM, AI, VC, " +
                            "      TAC, APP, FLAG, ERRNO)";
                }else {
                    if (outZipFileName.startsWith("CZ20650001")){
                        fieldNames = "(" +
                                "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                                "      PSN, LCN, FCN, " +
                                "      LPID, LTIM, PID, TIM, " +
                                "      TF, BAL, TT, RN, " +
                                "      EPID, ETIM, AI, VC, " +
                                "      TAC, APP constant '10', FLAG constant '1', ERRNO constant '00')";
                    }else {
                        fieldNames = "(" +
                                "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                                "      PSN, LCN, FCN, " +
                                "      LPID, LTIM, PID, TIM, " +
                                "      TF, BAL, TT, RN, " +
                                "      EPID, ETIM, AI, VC, " +
                                "      TAC, APP constant '10', FLAG, ERRNO)";
                    }
                }
                //控制文件
                contlFile = new File(sqlldrDir,"mCardInvest.ctl");

                boolean f1 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,targetFile);
                if (!f1){
                    //修改
                    processResultService.update(
                            new FileProcessResult(outZipFileName, targetFile.getAbsolutePath(), new Date(),
                                    "6555", "落库失败"));
                    return false;
                }
                //汇总
                CountData countData = mCardInvestMapper.countAllData(date,outZipFileName);
                long inputInvestNotes = 0L;
                BigDecimal inputInvestAmount = new BigDecimal("0");
                if (countData != null){
                    inputInvestNotes = countData.getNotesSum();
                    inputInvestAmount = countData.getAmountSum();
                }
                boolean f2 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,unZipFile);
                if (!f2){
                    //修改
                    processResultService.update(
                            new FileProcessResult(outZipFileName, unZipFile.getAbsolutePath(), new Date(),
                                    "6555", "落库失败"));
                    return false;
                }
                //汇总
                CountData oCountData = mCardInvestMapper.countAllData(date, outZipFileName);
                long outputInvestNotes = 0L;
                BigDecimal outputInvestAmount = new BigDecimal("0");
                if (countData != null){
                    outputInvestNotes = oCountData.getNotesSum();
                    outputInvestAmount = oCountData.getAmountSum();
                }
                if (inputInvestNotes != outputInvestNotes || inputInvestAmount.compareTo(outputInvestAmount) != 0){
                    log.error("input文件{}的笔数或者金额不等于output对应的文件。input笔数：{}，金额:{}。output笔数：{}，金额:{}",
                            unZipFile.getAbsolutePath(),inputInvestNotes,inputInvestAmount,outputInvestNotes,outputInvestAmount);
                    //插入文件检查表
                    processResultService.delAndInsert(new FileCheckError(date,unZipFile.getAbsolutePath(),"01","02",new Date(),
                            "6555","input文件的笔数或者金额不等于output对应的文件",inputInvestNotes,
                            inputInvestAmount,outputInvestNotes,outputInvestAmount,0L,0L,
                            new BigDecimal("0"),new BigDecimal("0")));
                    //修改
                    processResultService.update(
                            new FileProcessResult(outZipFileName, unZipFile.getAbsolutePath(), new Date(),
                                    "6555", "文件交易笔数，金额校验失败"));
                }else {
                    log.info("{}校验成功。",outZipFileName);
                }
                return true;
            }else if (fileName.startsWith("CZ")){
                tableName = "T_MCARD_INVEST_CHECKBACK";
                if (isNewCw){
                    fieldNames = "(" +
                            "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                            "      PSN , LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP, FLAG, ERRNO)";
                }else {
                    fieldNames = "(" +
                            "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                            "      PSN constant '00000000', LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP constant '10', FLAG, ERRNO)";
                }
                //控制文件
                contlFile = new File(sqlldrDir,"mCardInvestCheckBack.ctl");
            }else if (fileName.startsWith("XZ")){
                tableName = "T_MCARD_INVEST_REVISE_HIS";
                if (isNewCw){
                    fieldNames = "(" +
                            "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                            "      PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      EPID, ETIM, AI, VC, " +
                            "      TAC, APP, FLAG, ERRNO)";
                }else {
                    fieldNames = "(" +
                            "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                            "      PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      EPID, ETIM, AI, VC, " +
                            "      TAC, APP constant '10', FLAG, ERRNO)";
                }
                //控制文件
                contlFile = new File(sqlldrDir,"mCardInvestReviseHis.ctl");
            }else if (fileName.startsWith("LC")){
                tableName = "T_MCARD_INVEST_CHECKBACK_HIS";
                if (isNewCw){
                    fieldNames = "(" +
                            "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                            "      PSN , LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP, FLAG, ERRNO)";
                }else {
                    fieldNames = "(" +
                            "      SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+outZipFileName+"," +
                            "      PSN constant '00000000', LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP constant '10', FLAG, ERRNO)";
                }
                //控制文件
                contlFile = new File(sqlldrDir,"mCardInvestCheckBackHis.ctl");
            }
        }
        boolean flag = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,unZipFile);
        if (!flag){
            //修改
            processResultService.update(
                    new FileProcessResult(outZipFileName, unZipFile.getAbsolutePath(), new Date(),
                            "6555", "落库失败"));
        }
        return flag;
    }

    /**
     *  把CX或者XF压缩文件中的JY文件和对应的output文件夹的相关的文件（CW,XZ,QS）落库
     * @param unOutZipFileDir input对应的output的文件夹
     * @param inZipFileName  input压缩文件名字
     * @param unZipFile 解压后文件
     * @param dbUser
     * @param dbPassword
     * @param odbName
     * @return 全部落库是否成功
     */
    public boolean batchInsertConsumerData(String date,File unOutZipFileDir, String inZipFileName, File unZipFile, Boolean isBusFile,
                               File sqlldrDir, String dbUser, String dbPassword, String odbName) {
        List<Map<String, Object>> info = new ArrayList<>();
        //表名
        String tableName = null;
        //表字段
        String fieldNames = null;
        //控制文件
        File contlFile = null;
        //QS文件落库
        for (File outputUnzipFile : unOutZipFileDir.listFiles()) {
            if (outputUnzipFile.getName().startsWith("QS")) { //清算
                if (inZipFileName.startsWith("XF268")){
                    File qs = new File(unOutZipFileDir,"qs.txt");
                    //转换
                    convertTo268Qs(outputUnzipFile,qs);
                    outputUnzipFile = qs;
                }
                String otable = "T_SETTLE_AUDIT";
                String ofields = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                        "       ARSN, PID, SPT, SRT, TPC, TRC)";
                File ocfile = new File(sqlldrDir, "settleAudit.ctl");
                toMap(info, otable, ofields, ocfile, outputUnzipFile);
                break;
            }
        }
        if (inZipFileName.startsWith("CX")) { //CPU卡
            if (isBusFile) {  //公交
                tableName = "T_CPU_CONSUME";
                fieldNames = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                        "PID,PSN,TIM,LCN,FCN,TF,FEE,BAL,TT,ATT,CRN,XRN,DMON,BDCT," +
                        "MDCT,UDCT,EPID,ETIM,LPID,LTIM,AREA,ACT,SAREA,TAC,MEM)";
                //控制文件
                contlFile = new File(sqlldrDir, "cpuConsume.ctl");
                for (File outputUnzipFile : unOutZipFileDir.listFiles()) {
                    if (outputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_CPU_CONSUME_ERROR";
                        String ofields = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                                "PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, BDCT, MDCT, " +
                                "UDCT, EPID, ETIM, LPID, LTIM, AREA, ACT, SAREA, TAC, STATUS)";
                        File ocfile = new File(sqlldrDir, "cpuConsumeError.ctl");
                        toMap(info, otable, ofields, ocfile, outputUnzipFile);
                    }
                    if (outputUnzipFile.getName().startsWith("XZ")) { //修正
                        String otable = "T_CPU_CONSUME_REVISE";
                        String ofields = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                                "FNAME, PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, BDCT, MDCT, " +
                                "UDCT, EPID, ETIM, LPID, LTIM, AREA, ACT, SAREA, TAC, FLAG, CODE)";
                        File ocfile = new File(sqlldrDir, "cpuConsumeRevise.ctl");
                        toMap(info, otable, ofields, ocfile, outputUnzipFile);
                    }
                }
            } else { //非公交
                tableName = "T_CPU_CONSUME_NOBUS";
                fieldNames = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                        "PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, EPID, ETIM, LPID, LTIM, TAC)";
                //控制文件
                contlFile = new File(sqlldrDir, "cpuConsumeNoBus.ctl");
                for (File outputUnzipFile : unOutZipFileDir.listFiles()) {
                    if (outputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_CPU_CONSUME_ERROR_NOBUS";
                        String ofields = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                                "PID, PSN, TAC, STATUS)";
                        File ocfile = new File(sqlldrDir, "cpuConsumeErrorNoBus.ctl");
                        toMap(info, otable, ofields, ocfile, outputUnzipFile);
                        break;
                    }
                }
            }
        } else if (inZipFileName.startsWith("XF")) { //M1卡
            if (isBusFile) {  //公交
                tableName = "T_MCARD_CONSUME";
                fieldNames = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                        "PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, FEE, TT, " +
                        "RN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, AI, VC, TAC)";
                //控制文件
                contlFile = new File(sqlldrDir, "mCardConsume.ctl");
                for (File outputUnzipFile : unOutZipFileDir.listFiles()) {
                    if (outputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_MCARD_CONSUME_ERROR";
                        String ofields = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                                "PID, PSN, STATUS)";
                        File ocfile = new File(sqlldrDir, "mCardConsumeError.ctl");
                        //pid=cw的psn,psn=cw的pid,把它转换回来
                        if (inZipFileName.startsWith("XF0000") || inZipFileName.startsWith("XF0002") ||
                                inZipFileName.startsWith("XF90100001") || inZipFileName.startsWith("XF90130001")) {
                            File cw = new File(unOutZipFileDir, "cw.txt");
                            convertCw(outputUnzipFile, cw);
                            outputUnzipFile = cw;
                        }
                        if (inZipFileName.startsWith("XF268")) {
                            //特殊文件，CW与JY一致。pid=cw的psn,psn=cw的pid
                            File cw = new File(unOutZipFileDir, "cw.txt");
                            convertToCw(outputUnzipFile, cw);
                            outputUnzipFile = cw;
                        }
                        toMap(info, otable, ofields, ocfile, outputUnzipFile);
                        continue;
                    }
                    if (outputUnzipFile.getName().startsWith("XZ")) { //修正
                        String otable = "T_MCARD_CONSUME_REVISE";
                        String ofields = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                                "FNAME, PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, FEE, TT, RN, DMON, BDCT," +
                                " MDCT, UDCT, EPID, ETIM, AI, VC, TAC, FLAG, CODE)";
                        File ocfile = new File(sqlldrDir, "mCardConsumeRevise.ctl");
                        toMap(info, otable, ofields, ocfile, outputUnzipFile);
                    }
                }
            } else { //非公交
                tableName = "T_MCARD_CONSUME_NOBUS";
                //XF80480001的JY多个8位余额
                if (inZipFileName.startsWith("XF80480001")) {
                    fieldNames = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                            "PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL,FEE," +
                            " TT, RN, EPID, ETIM, AI, VC, TAC, MEM)";
                } else {
                    fieldNames = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                            "PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL,FEE constant '00000.00'," +
                            "TT, RN, EPID, ETIM, AI, VC, TAC, MEM)";
                }
                //控制文件
                contlFile = new File(sqlldrDir, "mCardConsumeNoBus.ctl");
                for (File outputUnzipFile : unOutZipFileDir.listFiles()) {
                    if (outputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_MCARD_CONSUME_ERROR_NOBUS";
                        String ofields = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                                "PSN, PID, STATUS)";
                        File ocfile = new File(sqlldrDir, "mCardConsumeErrorNoBus.ctl");
                        toMap(info, otable, ofields, ocfile, outputUnzipFile);
                        break;
                    }
                }
            }
        }
        toMap(info, tableName, fieldNames, contlFile, unZipFile);
        //落库
        for (Map<String, Object> map : info) {
            boolean f = SqlLdrUtil.insertBySqlLdr(dbUser, dbPassword, odbName, (String) map.get("tableName"), (String) map.get("fieldNames"),
                    (File) map.get("contlFile"), (File) map.get("dataFile"));
            if (!f) {
                log.error("落库失败，文件是{}", ((File) map.get("dataFile")).getAbsolutePath());
                //修改
                processResultService.update(
                        new FileProcessResult(inZipFileName, ((File) map.get("dataFile")).getAbsolutePath(), new Date(),
                                "6555", "落库失败"));
                return false;
            }
        }
        return true;

    }

    private void convertTo268Qs(File outputUnzipFile, File qs) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputUnzipFile)));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qs),"UTF-8"));
            String line = null,resultLine = null,sp = "\t";
            while ((line = reader.readLine()) != null){
                String[] split = line.split("\t");
                resultLine = new StringBuilder()
                        .append(split[0])
                        .append(sp)
                        .append(split[1])
                        .append(sp)
                        .append(split[3])
                        .append(sp)
                        .append(split[4])
                        .append(sp)
                        .append(split[5])
                        .append(sp)
                        .append(split[6])
                        .append(System.getProperty("line.separator"))
                        .toString();
                writer.write(resultLine);
            }
        }catch (Exception e) {
            log.error("转换qs文件{}发生异常:{}",outputUnzipFile.getAbsolutePath(),e);
        } finally {
            FileUtil.closeReader(reader);
            FileUtil.closeWriter(writer);
        }
    }

    private void convertCw(File cOutputUnzipFile, File cw) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(cOutputUnzipFile)));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cw),"UTF-8"));
            String line = null,resultLine = null;
            while ((line = reader.readLine()) != null){
                String[] split = line.split("\t");
                resultLine = split[1] + "\t" + split[0] + "\t" + split[2] + System.getProperty("line.separator");
                writer.write(resultLine);
            }
        }catch (Exception e) {
            log.error("转换文件[{}]发生异常:{}",cOutputUnzipFile.getAbsolutePath(),e);
        } finally {
            FileUtil.closeReader(reader);
            FileUtil.closeWriter(writer);
        }
    }


    private void convertToCw(File cOutputUnzipFile, File cw) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(cOutputUnzipFile)));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cw),"UTF-8"));
            String line = null,resultLine = null;
            while ((line = reader.readLine()) != null){
                String[] split = line.split("\t");
                resultLine = split[5] + "\t" + split[0] + System.getProperty("line.separator");
                writer.write(resultLine);
            }
        }catch (Exception e) {
            log.error("转换文件{}发生异常:{}",cOutputUnzipFile.getAbsolutePath(),e);
        } finally {
            FileUtil.closeReader(reader);
            FileUtil.closeWriter(writer);
        }
    }


    private void toMap(List<Map<String, Object>> info, String otable, String ofields, File ocfile,File cOutputUnzipFile){
        Map<String,Object> map = new HashMap<>();
        map.put("tableName",otable);
        map.put("fieldNames",ofields);
        map.put("contlFile",ocfile);
        map.put("dataFile",cOutputUnzipFile);
        info.add(map);
    }

    /**
     * 客服数据包的JY文件数据落库
     * @param inZipFileName  input压缩文件名字
     * @param unZipFile 解压后文件
     * @param sqlldrDir
     * @param dbUser
     * @param dbPassword
     * @param odbName
     * @return 全部落库是否成功
     */
    @Override
    public boolean batchInsertCustomer(String date,String inZipFileName, File unZipFile, File sqlldrDir,
                               String dbUser, String dbPassword, String odbName) {
        //表名
        String tableName;
        //表字段
        String fieldNames;
        //控制文件
        File contlFile;
        if (inZipFileName.startsWith("CK")){
            tableName = "T_CPU_CUSTOMER_SERVICE";
            fieldNames = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                    "PID,PSN,TIM,LCN,FCN,TF,FEE,BAL,TT,ATT,CRN,XRN,DMON," +
                    "BDCT,MDCT,UDCT,EPID,ETIM,LPID,LTIM,AREA,ACT,SAREA,TAC)";
            //控制文件
            contlFile = new File(sqlldrDir,"cpuCustomerService.ctl");
        }else { //KF
            tableName = "T_MCARD_CUSTOMER_SERVICE";
            fieldNames = "(SETTLE_DATE constant "+date+",ZIP_FILE_NAME constant "+inZipFileName+"," +
                    "PSN, LCN, FCN, LPID, LTIM, PID, " +
                    "TIM, TF, BAL, TT, RN, EPID, ETIM, AI, VC, TAC)";
            //控制文件
            contlFile = new File(sqlldrDir,"mCardCustomerService.ctl");
        }
        boolean f = SqlLdrUtil.insertBySqlLdr(dbUser, dbPassword, odbName, tableName, fieldNames, contlFile, unZipFile);
        if (!f) {
            log.error("落库失败，文件是{}", unZipFile.getAbsolutePath());
            //修改
            processResultService.update(
                    new FileProcessResult(inZipFileName, unZipFile.getAbsolutePath(), new Date(),
                            "6555", "落库失败"));
        }
        return f;
    }

}
