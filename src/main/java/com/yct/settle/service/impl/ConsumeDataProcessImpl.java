package com.yct.settle.service.impl;

import com.yct.settle.mapper.*;
import com.yct.settle.pojo.*;
import com.yct.settle.service.ConsumeDataProcess;
import com.yct.settle.service.CustomerServiceDataProcess;
import com.yct.settle.service.ProcessResultService;
import com.yct.settle.thread.ThreadTaskHandle;
import com.yct.settle.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/2 11:41
 */
@Service
public class ConsumeDataProcessImpl implements ConsumeDataProcess {
    private final Logger log = LoggerFactory.getLogger(ConsumeDataProcessImpl.class);
    @Resource
    private ProcessResultService processResultService;
    @Resource
    private CpuConsumeMapper cpuConsumeMapper;
    @Resource
    private CpuConsumeNoBusMapper cpuConsumeNoBusMapper;
    @Resource
    private CpuConsumeReviseMapper cpuConsumeReviseMapper;
    @Resource
    private MCardConsumeMapper mCardConsumeMapper;
    @Resource
    private MCardConsumeNoBusMapper mCardConsumeNoBusMapper;
    @Resource
    private MCardConsumeReviseMapper mCardConsumeReviseMapper;
    @Resource
    private CustomerServiceDataProcess customerServiceDataProcess;
    @Resource
    private CpuCustomerServiceMapper cpuCustomerServiceMapper;
    @Resource
    private MCardCustomerServiceMapper mCardCustomerServiceMapper;
    @Resource
    private ThreadTaskHandle threadTaskHandle;
    @Resource
    private SettleAuditMapper settleAuditMapper;
    @Resource
    private FileCheckErrorMapper fileCheckErrorMapper;



    /**
     * 处理消费，客服文件
     * @param inputDataFolder
     * @param outputDataFolder
     * @param date
     * @param dmcj
     * @param dmcx
     * @param dmmj
     * @param dmmx
     * @param dbUser
     * @param dbPassword
     * @param odbName
     * @param sqlldrDir
     * @param resultMap
     * @return
     */
    @Override
    public boolean processConsumeFiles(String inputDataFolder, String outputDataFolder, String date,
                                       File dmcj, File dmcx, File dmmj, File dmmx, String dbUser,
                                       String dbPassword, String odbName, File sqlldrDir, Map<String, String> resultMap) {
        String inZipFileName = null;
        File unZipDir = null;
        try {
            //处理消费文件
            File inputDateDir = new File(inputDataFolder + File.separator + date);
            log.info("开始处理文件夹{}下的消费文件",inputDateDir.getName());
            if (inputDateDir.exists()) {
                Boolean isBusFile = false;
                //找到对应output日期文件夹
                File cOutputDateDir = new File(outputDataFolder + File.separator + date);
                File[] inZipFiles = inputDateDir.listFiles();
                for (File inZipFile : inZipFiles) {

                    //其他线程检查
                    if (threadTaskHandle.getIsError()) {
                        log.info("有线程发生了异常，处理充值文件的线程无需再执行！");
                        return false;
                    }

                    inZipFileName = inZipFile.getName();
                    //处理inputDate下的一个压缩文件
                    if (inZipFileName.startsWith("CX") || inZipFileName.startsWith("XF") ||
                            inZipFileName.startsWith("CK") || inZipFileName.startsWith("KF")) {
                        String zipFileType,cardType;
                        //落库文件处理表
                        FileProcessResult result = new FileProcessResult();
                        result.setSettleDate(date);
                        if (inZipFileName.startsWith("CX")){
                            //cpu卡，消费
                            cardType = "01";
                            zipFileType = "02";
                        }else if (inZipFileName.startsWith("XF")){
                            //m1卡，消费
                            cardType = "02";
                            zipFileType = "02";
                        }else if (inZipFileName.startsWith("CK")){
                            //cpu卡，客服
                            cardType = "01";
                            zipFileType = "03";
                        }else {
                            //m1卡，客服
                            cardType = "02";
                            zipFileType = "03";
                        }
                        result.setZipFileType(zipFileType);
                        result.setZipFileName(inZipFileName);
                        result.setCardType(cardType);
                        processResultService.delAndInsert(result);
                        Boolean lean = FileUtil.unZip(inZipFile);
                        if (lean) {
                            //进入解压后的文件夹
                            String unZipDirName = inZipFile.getName().substring(0, inZipFile.getName().indexOf("."));
                            unZipDir = new File(inputDateDir, unZipDirName);
                            File[] unZipFiles = unZipDir.listFiles();
                            boolean b = false;
                            boolean jyIsNull = false;
                            for (File unZipFile : unZipFiles) {
                                if ("02".equals(zipFileType) && unZipFile.getName().startsWith("JY")) { //消费
                                    if (unZipFile.length() > 0){    //JY为空
                                        isBusFile = FileUtil.isBusFile(unZipDirName, unZipFile);
                                        //落库
                                        b = batchInsert(cOutputDateDir, unZipDirName, unZipFile, isBusFile, sqlldrDir,
                                                dbUser,dbPassword,odbName);
                                    }else {
                                        b = true;
                                        jyIsNull = true;
                                    }
                                }else  if ("03".equals(zipFileType) && unZipFile.getName().startsWith("JY")) { //客服
                                    b = customerServiceDataProcess.batchInsert(unZipDirName, unZipFile, sqlldrDir,
                                            dbUser,dbPassword,odbName);
                                }else {
                                    continue;
                                }
                                if (!b){
                                    processResultService.update(new FileProcessResult(inZipFileName, unZipFile.getAbsolutePath(),new Date(),"6555","落库失败"));
                                    //删除解压的文件夹
                                    FileUtil.deleteFile(unZipDir);
                                    return false;
                                }
                                break;  //只需要处理JY文件
                            }
                            if (jyIsNull){
                                FileUtil.deleteFile(unZipDir);
                                continue;
                            }
                            //对消费进行文件内容校验,并把这个消费文件的统计数据存到数据库
                            boolean audit = auditAndInsert(inZipFileName,isBusFile,date);
                            if (audit){
                                //从数据库取出数据写入dm
                                writerConsumeOrCustomerToDm(dmmj,dmmx,dmcj,dmcx,date,inZipFileName,isBusFile,zipFileType);
                                //删除解压的文件夹
                                FileUtil.deleteFile(unZipDir);
                            }else {
                                threadTaskHandle.setIsError(true);
                                log.error("处理消费文件{}发生异常,修改标志，通知其他线程", inZipFile);
                                //删除解压的文件夹
                                FileUtil.deleteFile(unZipDir);
                                //修改
                                processResultService.update(new FileProcessResult(inZipFileName,inZipFile.getAbsolutePath(),new Date(),"6555","文件内容有误"));
                                return false;
                            }
                        } else {
                            threadTaskHandle.setIsError(true);
                            log.error("处理消费文件{}发生异常,修改标志，通知其他线程", inZipFile);
                            //删除解压的文件夹
                            FileUtil.deleteFile(unZipDir);
                            //修改
                            processResultService.update(new FileProcessResult(inZipFileName,inZipFile.getAbsolutePath(),new Date(),"6555","解压失败"));
                            return false;
                        }
                    }
                }
            }
        }catch (Exception e){
            threadTaskHandle.setIsError(true);
            log.error("处理消费文件{}发生异常：{},修改标志，通知其他线程", inZipFileName, e);
            //删除解压的文件夹
            FileUtil.deleteFile(unZipDir);
            //修改
            processResultService.update(new FileProcessResult(inZipFileName,null,new Date(),
                        "6555","处理消费或者客服文件发生异常"));
            return false;
        }
        resultMap.put("consumeResultCode","0000");
        return true;
    }

    private boolean auditAndInsert(String inZipFileName, boolean isBusFile, String date) {
        long investNotes = 0L;
        BigDecimal investAmount = new BigDecimal("0");
        long consumeNotes = 0L;
        BigDecimal consumeAmount = new BigDecimal("0");
        long reviseNotes = 0L;
        BigDecimal reviseAmount = new BigDecimal("0");

        if (inZipFileName.startsWith("CX")){    //cpu消费文件
            if (isBusFile){ //公交
                log.info("cpu卡公交消费文件内容校验");
                long cpuCwNotes = cpuConsumeMapper.findCwNotes();
                long cwNotes = cpuConsumeMapper.countCwNotes();
                //校验错误笔数
                if (cpuCwNotes == cwNotes){
                    //校验清算金额
                    BigDecimal qsTotalAmount = settleAuditMapper.countTotalAmount();
                    if (qsTotalAmount == null){
                        qsTotalAmount = new BigDecimal("0");
                    }
                    CountData cpuConsumeCountData = cpuConsumeMapper.countAmountAndNum();
                    if (cpuConsumeCountData == null){
                        cpuConsumeCountData.setAmountSum(new BigDecimal("0"));
                    }
                    if (qsTotalAmount.compareTo(cpuConsumeCountData.getAmountSum()) == 0){
                        log.info("校验成功。cpu卡公交消费文件统计并落库");
                        consumeNotes = cpuConsumeCountData.getNotesSum();
                        consumeAmount = cpuConsumeCountData.getAmountSum();
                        CountData cpuConsumeReviseCountData = cpuConsumeReviseMapper.countAmountAndNum();
                        reviseNotes = cpuConsumeReviseCountData.getNotesSum();
                        reviseAmount = cpuConsumeReviseCountData.getAmountSum();
                    }else {
                        String msg = "cpu卡公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                        log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,cpuConsumeCountData.getAmountSum());
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                                msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                qsTotalAmount,cpuConsumeCountData.getConsumeAmount()));
                        return false;
                    }
                }else {
                    String msg = "cpu卡公交消费文件错误文件的笔数与消费文件错误笔数不符";
                    log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,cpuCwNotes);
                    fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                            msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                            null,null));
                    return false;
                }
            }else { //cpu卡非公交
                log.info("cpu卡非公交消费文件内容校验");
                long cpuCwNotes = cpuConsumeNoBusMapper.findCwNotes();
                long cwNotes = cpuConsumeNoBusMapper.countCwNotes();
                //校验错误笔数
                if (cpuCwNotes == cwNotes) {
                    //校验清算金额
                    BigDecimal qsTotalAmount = settleAuditMapper.countTotalAmount();
                    if (qsTotalAmount == null){
                        qsTotalAmount = new BigDecimal("0");
                    }
                    CountData cpuConsumeCountData = cpuConsumeNoBusMapper.countAmountAndNum();
                    if (cpuConsumeCountData == null){
                        cpuConsumeCountData.setAmountSum(new BigDecimal("0"));
                    }
                    if (qsTotalAmount.compareTo(cpuConsumeCountData.getAmountSum()) == 0) {
                        log.info("校验成功。cpu卡非公交消费文件统计并落库");
                        consumeNotes = cpuConsumeCountData.getNotesSum();
                        consumeAmount = cpuConsumeCountData.getAmountSum();
                    }else {
                        String msg = "cpu卡非公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                        log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,cpuConsumeCountData.getAmountSum());
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                                msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                qsTotalAmount,cpuConsumeCountData.getConsumeAmount()));
                        return false;
                    }
                }else {
                    String msg = "cpu卡非公交消费文件错误文件的笔数与消费文件错误笔数不符";
                    log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,cpuCwNotes);
                    fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                            msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                            null,null));
                    return false;
                }
            }
        }else if (inZipFileName.startsWith("XF")){  //m1卡
            if (isBusFile){
                log.info("m1卡公交消费文件内容校验");
                long cpuCwNotes = mCardConsumeMapper.findCwNotes();
                long cwNotes = mCardConsumeMapper.countCwNotes();
                //校验错误笔数
                if (cpuCwNotes == cwNotes) {
                    //校验清算金额
                    BigDecimal qsTotalAmount = null;
                    if (inZipFileName.startsWith("XF268")){
                        qsTotalAmount = settleAuditMapper.countTotalAmountZ();
                    }else {
                        qsTotalAmount = settleAuditMapper.countTotalAmount();
                    }
                    if (qsTotalAmount == null){
                        qsTotalAmount = new BigDecimal("0");
                    }
                    CountData cpuConsumeCountData = mCardConsumeMapper.countAmountAndNum();
                    if (cpuConsumeCountData == null) {
                        cpuConsumeCountData.setAmountSum(new BigDecimal("0"));
                    }
                    if (qsTotalAmount.compareTo(cpuConsumeCountData.getAmountSum()) == 0) {
                        log.info("校验成功.m1卡公交消费文件统计数据并落库");
                        CountData mCardConsume = mCardConsumeMapper.countAmountAndNum();
                        consumeNotes = mCardConsume.getNotesSum();
                        consumeAmount = mCardConsume.getAmountSum();
                        CountData cpuConsumeRevise = mCardConsumeReviseMapper.countAmountAndNum();
                        reviseNotes = cpuConsumeRevise.getNotesSum();
                        reviseAmount = cpuConsumeRevise.getAmountSum();
                    }else {
                        String msg = "m1卡公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                        log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,cpuConsumeCountData.getAmountSum());
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                                msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                qsTotalAmount,cpuConsumeCountData.getConsumeAmount()));
                        return false;
                    }
                }else {
                    String msg = "m1卡公交消费文件错误文件的笔数与消费文件错误笔数不符";
                    log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,cpuCwNotes);
                    fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                            msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                            null,null));
                    return false;
                }
            }else {
                log.info("m1卡非公交消费文件内容校验");
                long cpuCwNotes = mCardConsumeNoBusMapper.findCwNotes();
                long cwNotes = mCardConsumeNoBusMapper.countCwNotes();
                //校验错误笔数
                if (cpuCwNotes == cwNotes) {
                    //校验清算金额
                    BigDecimal qsTotalAmount = settleAuditMapper.countTotalAmount();
                    if (qsTotalAmount == null){
                        qsTotalAmount = new BigDecimal("0");
                    }
                    CountData cpuConsumeCountData = mCardConsumeNoBusMapper.countAmountAndNum();
                    if (cpuConsumeCountData == null) {
                        cpuConsumeCountData.setAmountSum(new BigDecimal("0"));
                    }
                    if (qsTotalAmount.compareTo(cpuConsumeCountData.getAmountSum()) == 0) {
                        log.info("校验成功。m1卡非公交消费文件统计数据并落库");
                        CountData mCardConsume = mCardConsumeNoBusMapper.countAmountAndNum();
                        consumeNotes = mCardConsume.getNotesSum();
                        consumeAmount = mCardConsume.getAmountSum();
                    }else {
                        String msg = "m1卡非公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                        log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,cpuConsumeCountData.getAmountSum());
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                                msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                qsTotalAmount,cpuConsumeCountData.getConsumeAmount()));
                        return false;
                    }
                }else {
                    String msg = "m1卡非公交消费文件错误文件的笔数与消费文件错误笔数不符";
                    log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,cpuCwNotes);
                    fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01","6555",
                            msg,0L,null,0L,null,cpuCwNotes,cwNotes,
                            null,null));
                    return false;
                }
            }
        }else if (inZipFileName.startsWith("Ck")){ //cpu卡客服
            log.info("cpu卡客服文件统计数据并落库");
            //cpu客服充值
            CountData cpuInvest = cpuCustomerServiceMapper.countInvestAmountAndNum();
            CountData cpuConsume = cpuCustomerServiceMapper.countConsumeAmountAndNum();

            investNotes = cpuInvest.getNotesSum();
            investAmount = cpuInvest.getAmountSum();
            consumeNotes = cpuConsume.getNotesSum() ;
            consumeAmount = cpuConsume.getAmountSum();
        }else if (inZipFileName.startsWith("KF")){ //m1卡客服
            log.info("m1卡客服文件统计数据并落库");
            //m1客服充值
            CountData mCardInvest = mCardCustomerServiceMapper.countInvestAmountAndNum();
            CountData mCardConsume = mCardCustomerServiceMapper.countConsumeAmountAndNum();
            investNotes = mCardInvest.getNotesSum();
            investAmount =mCardInvest.getAmountSum();
            consumeNotes = mCardConsume.getNotesSum();
            consumeAmount = mCardConsume.getAmountSum();
        }
        processResultService.update(
                new FileProcessResult(inZipFileName,new Date(),"0000","处理成功", investNotes,investAmount,
                        consumeNotes,consumeAmount,reviseNotes,reviseAmount));
        return true;
    }

    private void writerConsumeOrCustomerToDm(File dmmj, File dmmx, File dmcj, File dmcx, String date,
                                             String inZipFileName, Boolean isBusFile,String zipFileType) {
        if ("02".equals(zipFileType)){  //消费
            //从数据库取出写入dm
            writerTODM(dmmj,dmmx,dmcj,dmcx,date,inZipFileName,isBusFile);
        }else if ("03".equals(zipFileType)){ //客服
            //从数据库取出写入dm
            customerServiceDataProcess.writerTODM(dmmj, dmcj, date, inZipFileName);
        }
    }


    /**
     *  把CX或者XF压缩文件中的JY文件和对应的output文件夹的相关的文件（CW,XZ,QS）落库
     * @param cOutputDateDir input对应的output的文件夹
     * @param unZipDirName  input解压缩文件文件名字
     * @param unZipFile 解压后文件
     * @param dbUser
     * @param dbPassword
     * @param odbName
     * @return 全部落库是否成功
     */
    @Override
    public boolean batchInsert(File cOutputDateDir, String unZipDirName, File unZipFile, Boolean isBusFile,
                               File sqlldrDir, String dbUser, String dbPassword, String odbName) {
        ArrayList<Map<String,Object>> info = new ArrayList<>();
        //表名
        String tableName = null;
        //表字段
        String fieldNames = null;
        //控制文件
        File contlFile = null;
        File[] cOutputUnzipFiles = null;
        File cOutputUnzipDir = null;
        File oFile = null;
        for (File file : cOutputDateDir.listFiles()){
            if (file.getName().startsWith(unZipDirName)){
                oFile = file;
                break;
            }
        }
        if (oFile != null) {
            //解压
            FileUtil.unZip(oFile);
            //output对应的文件
            cOutputUnzipDir = new File(cOutputDateDir, unZipDirName);
        }else {
            //如果为空，创建文件夹
            cOutputUnzipDir = new File(cOutputDateDir, unZipDirName);
            cOutputUnzipDir.mkdir();
        }
        cOutputUnzipFiles = cOutputUnzipDir.listFiles();
        //没有CW,XZ,QS文件则创建空文件
        List<String> list = new ArrayList<>();
        for (File file  : cOutputUnzipFiles){
            list.add(file.getName().substring(0,2));
        }
        if (!list.contains("CW")){
            File cw = new File(cOutputUnzipDir,"CW.txt");
            try {
                cw.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!list.contains("XZ")){
            File xz = new File(cOutputUnzipDir,"XZ.txt");
            try {
                xz.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!list.contains("QS")){
            File qs = new File(cOutputUnzipDir,"QS.txt");
            try {
                qs.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //QS文件落库
        cOutputUnzipFiles = cOutputUnzipDir.listFiles();
        for (File cOutputUnzipFile : cOutputUnzipFiles) {
            if (cOutputUnzipFile.getName().startsWith("QS")) { //错误
                String otable = "T_SETTLE_AUDIT";
                String ofields = "(ARSN, PID, SPT, SRT, TPC, TRC)";
                File ocfile = new File(sqlldrDir, "settleAudit.ctl");
                toMap(info, otable, ofields, ocfile, cOutputUnzipFile);
                break;
            }
        }

        if (unZipDirName.startsWith("CX")) { //CPU卡
            if (isBusFile) {  //公交
                tableName = "T_CPU_CONSUME";
                fieldNames = "(PID,PSN,TIM,LCN,FCN,TF,FEE,BAL,TT,ATT,CRN,XRN,DMON,BDCT,MDCT,UDCT,EPID,ETIM,LPID,LTIM,AREA,ACT,SAREA,TAC,MEM)";
                //控制文件
                contlFile = new File(sqlldrDir, "cpuConsume.ctl");
                for (File cOutputUnzipFile : cOutputUnzipFiles) {
                    if (cOutputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_CPU_CONSUME_ERROR";
                        String ofields = "(PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, LPID, LTIM, AREA, ACT, SAREA, TAC, STATUS)";
                        File ocfile = new File(sqlldrDir, "cpuConsumeError.ctl");
                        toMap(info, otable, ofields, ocfile, cOutputUnzipFile);
                    }
                    if (cOutputUnzipFile.getName().startsWith("XZ")) { //修正
                        String otable = "T_CPU_CONSUME_REVISE";
                        String ofields = "(FNAME, PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, LPID, LTIM, AREA, ACT, SAREA, TAC, FLAG, CODE)";
                        File ocfile = new File(sqlldrDir, "cpuConsumeRevise.ctl");
                        toMap(info, otable, ofields, ocfile, cOutputUnzipFile);
                    }
                }
            } else { //非公交
                tableName = "T_CPU_CONSUME_NOBUS";
                fieldNames = "(PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, EPID, ETIM, LPID, LTIM, TAC)";
                //控制文件
                contlFile = new File(sqlldrDir, "cpuConsumeNoBus.ctl");
                for (File cOutputUnzipFile : cOutputUnzipFiles) {
                    if (cOutputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_CPU_CONSUME_ERROR_NOBUS";
                        String ofields = "(PID, PSN, TAC, STATUS)";
                        File ocfile = new File(sqlldrDir, "cpuConsumeErrorNoBus.ctl");
                        toMap(info, otable, ofields, ocfile, cOutputUnzipFile);
                        break;
                    }
                }
            }
        } else if (unZipDirName.startsWith("XF")) { //M1卡
            if (isBusFile) {  //公交
                tableName = "T_MCARD_CONSUME";
                fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, FEE, TT, RN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, AI, VC, TAC)";
                //控制文件
                contlFile = new File(sqlldrDir, "mCardConsume.ctl");
                for (File cOutputUnzipFile : cOutputUnzipFiles) {
                    if (cOutputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_MCARD_CONSUME_ERROR";
                        String ofields = "(PID, PSN, STATUS)";
                        File ocfile = new File(sqlldrDir, "mCardConsumeError.ctl");
                        //pid=cw的psn,psn=cw的pid,把它转换回来
                        if (unZipDirName.startsWith("XF0000") || unZipDirName.startsWith("XF0002")) {
                            File cw = new File(cOutputUnzipDir, "cw.txt");
                            convertCw(cOutputUnzipFile, cw);
                            cOutputUnzipFile = cw;
                        }
                        if (unZipDirName.startsWith("XF268")) {
                            //特殊文件，CW与JY一致。pid=cw的psn,psn=cw的pid
                            File cw = new File(cOutputUnzipDir, "cw.txt");
                            convertToCw(cOutputUnzipFile, cw);
                            cOutputUnzipFile = cw;
                        }
                        toMap(info, otable, ofields, ocfile, cOutputUnzipFile);
                        continue;
                    }
                    if (cOutputUnzipFile.getName().startsWith("XZ")) { //修正
                        String otable = "T_MCARD_CONSUME_REVISE";
                        String ofields = "(FNAME, PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, FEE, TT, RN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, AI, VC, TAC, FLAG, CODE)";
                        File ocfile = new File(sqlldrDir, "mCardConsumeRevise.ctl");
                        toMap(info, otable, ofields, ocfile, cOutputUnzipFile);
                    }
                }
            } else { //非公交
                tableName = "T_MCARD_CONSUME_NOBUS";
                //XF80480001的JY多个8位余额
                if (unZipDirName.startsWith("XF80480001")) {
                    fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL,FEE, TT, RN, EPID, ETIM, AI, VC, TAC, MEM)";
                } else {
                    fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL,FEE constant '00000.00',TT, RN, EPID, ETIM, AI, VC, TAC, MEM)";
                }
                //控制文件
                contlFile = new File(sqlldrDir, "mCardConsumeNoBus.ctl");
                for (File cOutputUnzipFile : cOutputUnzipFiles) {
                    if (cOutputUnzipFile.getName().startsWith("CW")) { //错误
                        String otable = "T_MCARD_CONSUME_ERROR_NOBUS";
                        String ofields = "(PSN, PID, STATUS)";
                        File ocfile = new File(sqlldrDir, "mCardConsumeErrorNoBus.ctl");
                        toMap(info, otable, ofields, ocfile, cOutputUnzipFile);
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
                FileUtil.deleteFile(cOutputUnzipDir);
                return false;
            }
        }
        FileUtil.deleteFile(cOutputUnzipDir);
        return true;

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
                resultLine = split[1] + "\t" + split[0] + System.getProperty("line.separator");
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
     * 从数据库取数据写到dm
     * @param dmmj
     * @param dmmx
     * @param dmcj
     * @param dmcx
     * @param settleDate    结算日期
     * @param zipFileName   压缩文件名称
     * @param isBusFile     是否是公交文件
     */
    @Override
    public void writerTODM(File dmmj, File dmmx, File dmcj, File dmcx,
                                    String settleDate, String zipFileName, Boolean isBusFile) {
        log.info("从数据库取消费数据写到对应的dm文件");
        if (zipFileName.startsWith("CX")){ //cpu卡
            ArrayList<CpuTrade> cpuTradeList = new ArrayList<>();
            if (isBusFile){ //公交
                //获取总笔数
                long allNotes = cpuConsumeMapper.findAllNotes();
                if (allNotes > 0L) {
                    long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                    while (allNotes > 0L) {
                        if (allNotes <= pageSize) {
                            pageSize = allNotes;
                        }
                        endNum = startNum + pageSize;
                        log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                        List<CpuConsume> cpuConsumeList = cpuConsumeMapper.findByWhere(startNum,endNum);
                        for (CpuConsume cpuConsume : cpuConsumeList){
                            CpuTrade cpuTrade = new CpuTrade();
                            convertToCpuTrade(cpuConsume,cpuTrade,settleDate,zipFileName);
                            cpuTradeList.add(cpuTrade);
                        }
                        FileUtil.writeToFile(dmcj,cpuTradeList);
                        cpuTradeList.clear();
                        allNotes -= pageSize;
                        startNum += pageSize;
                        count++;
                    }
                }
                //修正
                List<CpuConsumeRevise> list = cpuConsumeReviseMapper.findList();
                if (list.size() > 0){
                    ArrayList<CpuTradeRevise> cpuTradeReviseList = new ArrayList<>();
                    for (CpuConsumeRevise cpuConsumeRevise : list){
                        CpuTradeRevise cpuTradeRevise = new CpuTradeRevise();
                        convertToCpuTradeRevise(cpuConsumeRevise,cpuTradeRevise,settleDate,zipFileName);
                        cpuTradeReviseList.add(cpuTradeRevise);
                    }
                    FileUtil.writeToFile(dmcx,cpuTradeReviseList);
                }
            }else {
                //获取总笔数
                long allNotes = cpuConsumeNoBusMapper.findAllNotes();
                if (allNotes > 0L) {
                    long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                    while (allNotes > 0L) {
                        if (allNotes <= pageSize) {
                            pageSize = allNotes;
                        }
                        endNum = startNum + pageSize;
                        log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                        List<CpuConsumeNoBus> cpuConsumeNoBusList = cpuConsumeNoBusMapper.findByWhere(startNum,endNum);
                        for (CpuConsumeNoBus cpuConsumeNoBus : cpuConsumeNoBusList){
                            CpuTrade cpuTrade = new CpuTrade();
                            convertToCpuTrade(cpuConsumeNoBus,cpuTrade,settleDate,zipFileName);
                            cpuTradeList.add(cpuTrade);
                        }
                        FileUtil.writeToFile(dmcj,cpuTradeList);
                        cpuTradeList.clear();
                        allNotes -= pageSize;
                        startNum += pageSize;
                        count++;
                    }
                }
            }
        }else { //M1卡
            ArrayList<MCardTrade> mCardTradeList = new ArrayList<>();
            if (isBusFile){ //公交
                //获取总笔数
                long allNotes = mCardConsumeMapper.findAllNotes();
                if (allNotes > 0L){
                    long pageSize = 100000L,startNum = 0L,endNum,count = 1L;
                    while (allNotes > 0L){
                        if (allNotes <= pageSize){
                            pageSize = allNotes;
                        }
                        endNum = startNum + pageSize;
                        log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}",count,allNotes,pageSize,startNum,endNum);
                        List<MCardConsume> mCardConsumeList = mCardConsumeMapper.findByWhere(startNum,endNum);
                        for (MCardConsume mCardConsume : mCardConsumeList){
                            MCardTrade mCardTrade = new MCardTrade();
                            convertToMCardTrade(mCardConsume,mCardTrade,settleDate,zipFileName);
                            mCardTradeList.add(mCardTrade);
                        }
                        FileUtil.writeToFile(dmmj,mCardTradeList);
                        mCardTradeList.clear();
                        allNotes -= pageSize;
                        startNum += pageSize;
                        count++;
                    }
                }

                //修正
                List<MCardConsumeRevise> reviseList = mCardConsumeReviseMapper.findList();
                if (reviseList.size() > 0){
                    ArrayList<MCardTradeRevise> mCardTradeReviseList = new ArrayList<>();
                    for (MCardConsumeRevise mCardConsumeRevise : reviseList){
                        MCardTradeRevise mCardTradeRevise = new MCardTradeRevise();
                        convertToMCardTradeRevise(mCardConsumeRevise,mCardTradeRevise,settleDate,zipFileName);
                        mCardTradeReviseList.add(mCardTradeRevise);
                    }
                    FileUtil.writeToFile(dmmx,mCardTradeReviseList);
                }
            }else { //非公交
                //获取总笔数
                long allNotes = mCardConsumeNoBusMapper.findAllNotes();
                if (allNotes > 0L) {
                    long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                    while (allNotes > 0L) {
                        if (allNotes <= pageSize) {
                            pageSize = allNotes;
                        }
                        endNum = startNum + pageSize;
                        log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                        List<MCardConsumeNoBus> list = mCardConsumeNoBusMapper.findByWhere(startNum,endNum);
                        for (MCardConsumeNoBus mCardConsumeNoBus : list){
                            MCardTrade mCardTrade = new MCardTrade();
                            convertToMCardTrade(mCardConsumeNoBus,mCardTrade,settleDate,zipFileName);
                            mCardTradeList.add(mCardTrade);
                        }
                        FileUtil.writeToFile(dmmj,mCardTradeList);
                        mCardTradeList.clear();
                        allNotes -= pageSize;
                        startNum += pageSize;
                        count++;
                    }
                }
            }
        }
    }


    private void convertToMCardTradeRevise(MCardConsumeRevise mCardConsumeRevise, MCardTradeRevise mCardTradeRevise, String settleDate, String zipFileName) {
        BeanUtils.copyProperties(mCardConsumeRevise,mCardTradeRevise);
        mCardTradeRevise.setQDATE(settleDate);
        mCardTradeRevise.setQNAME(zipFileName);
        mCardTradeRevise.setLTIME(DateUtil.getTomorrow(mCardConsumeRevise.getTIM()));
        mCardTradeRevise.setDT("02");
        mCardTradeRevise.setTT(mCardConsumeRevise.getTT());
        mCardTradeRevise.setXT(mCardConsumeRevise.getFLAG());
        mCardTradeRevise.setDSN(mCardConsumeRevise.getPSN());
        mCardTradeRevise.setICN(mCardConsumeRevise.getLCN());
        mCardTradeRevise.setBINF("00000000000000000000");//备用信息
    }

    private void convertToMCardTrade(Object object, MCardTrade mCardTrade, String settleDate, String zipFileName) {
        if (object instanceof MCardConsume){
            MCardConsume mCardConsume = (MCardConsume) object;
            BeanUtils.copyProperties(mCardConsume,mCardTrade);
            mCardTrade.setDSN(mCardConsume.getPSN());
            mCardTrade.setICN(mCardConsume.getLCN());
        }else if (object instanceof MCardConsumeNoBus){
            MCardConsumeNoBus mCardConsumeNoBus = (MCardConsumeNoBus) object;
            BeanUtils.copyProperties(mCardConsumeNoBus,mCardTrade);
            mCardTrade.setDSN(mCardConsumeNoBus.getPSN());
            mCardTrade.setICN(mCardConsumeNoBus.getLCN());
            mCardTrade.setDMON("0000");
            mCardTrade.setBDCT("000");
            mCardTrade.setMDCT("000");
            mCardTrade.setUDCT("000");
        }
        mCardTrade.setQDATE(settleDate);
        mCardTrade.setDT("02");//消费
        mCardTrade.setBINF("00000000000000000000");//备用信息
        mCardTrade.setQNAME(zipFileName);
    }

    private void convertToCpuTradeRevise(CpuConsumeRevise cpuConsumeRevise, CpuTradeRevise cpuTradeRevise, String settleDate, String zipFileName) {
        BeanUtils.copyProperties(cpuConsumeRevise,cpuTradeRevise);
        cpuTradeRevise.setQDATE(settleDate);
        cpuTradeRevise.setLTIME(DateUtil.getTomorrow(cpuConsumeRevise.getTIM()));
        cpuTradeRevise.setDT("02");//消费
        cpuTradeRevise.setDMON("0000000000000");//扩展信息
        cpuTradeRevise.setBINF("00000000000000000000");//备用信息
        cpuTradeRevise.setQNAME(zipFileName);
        cpuTradeRevise.setXT(cpuConsumeRevise.getFLAG());

    }

    private void convertToCpuTrade(Object object, CpuTrade cpuTrade,String settleDate, String zipFileName) {
        if (object instanceof CpuConsume){
            CpuConsume cpuConsume = (CpuConsume) object;
            BeanUtils.copyProperties(cpuConsume,cpuTrade);
        }else if (object instanceof CpuConsumeNoBus){
            CpuConsumeNoBus cpuConsumeNoBus = (CpuConsumeNoBus) object;
            BeanUtils.copyProperties(cpuConsumeNoBus,cpuTrade);
        }
        cpuTrade.setQDATE(settleDate);
        cpuTrade.setDT("02");//消费
        cpuTrade.setTT("06");//消费
        cpuTrade.setDMON("0000000000000");//扩展信息
        cpuTrade.setQNAME(zipFileName);
    }
}
