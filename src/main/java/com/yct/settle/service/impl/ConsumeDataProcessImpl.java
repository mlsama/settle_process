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
        try {
            //处理消费文件
            File inputDateDir = new File(inputDataFolder + File.separator + date);
            if (inputDateDir.exists()) {
                log.info("开始处理文件夹{}下的消费文件",inputDateDir.getName());
                for (File inZipFile : inputDateDir.listFiles()) {

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

                       boolean insertFlag = insertConsumeOrCustomerData(date,inZipFile,outputDataFolder,zipFileType,
                                                            dbUser,dbPassword,odbName,sqlldrDir,resultMap);
                        if (!insertFlag){
                            threadTaskHandle.setIsError(true);
                            return false;
                        }
                        if ("yes".equals(resultMap.get("consumeJyIsNull"))){
                            continue;
                        }

                        //对消费进行文件内容校验,并把这个消费文件的统计数据存到数据库
                        String type = resultMap.get("isBusFile");
                        boolean isBusFile = false;
                        if ("yes".equals(type)){
                            isBusFile = true;
                        }
                        boolean audit = auditAndCount(inZipFileName,isBusFile,date,resultMap);
                        if (!audit){
                            threadTaskHandle.setIsError(true);
                            return false;
                        }
                        if ("yes".equals(resultMap.get("consumeProcessNextZipFile"))){
                            continue;
                        }
                        //从数据库取出数据写入dm
                        boolean writeFlag = writerConsumeOrCustomerToDm(dmmj,dmmx,dmcj,dmcx,date,inZipFileName,isBusFile,zipFileType);
                        if (!writeFlag){
                            threadTaskHandle.setIsError(true);
                            return false;
                        }
                    }
                }
            }
            resultMap.put("consumeResultCode","0000");
        }catch (Exception e){
            threadTaskHandle.setIsError(true);
            log.error("处理消费文件{}发生异常：{},修改标志，通知其他线程", inZipFileName, e);
            //修改
            processResultService.update(new FileProcessResult(inZipFileName,null,new Date(),
                        "6555","处理消费或者客服文件发生异常"));
            return false;
        }
        return true;
    }

    /**
     * 导入消费或者客服数据
     * @param date  清算日期
     * @param inZipFile input下的一个压缩文件
     * @param outputDataFolder  output文件夹
     * @param zipFileType   压缩文件类型
     * @param dbUser
     * @param dbPassword
     * @param odbName
     * @param sqlldrDir
     * @param resultMap
     * @return
     */
    private boolean insertConsumeOrCustomerData(String date, File inZipFile, String outputDataFolder, String zipFileType,
                                                String dbUser, String dbPassword, String odbName, File sqlldrDir, Map<String, String> resultMap) {
        File unInZipFileDir = null;   //input解压目录
        File unOutZipFileDir = null;  //output解压目录
        boolean isBusFile = false;    //是否是公交文件
        try {
            //处理已经解压过的文件夹
            inZipFile = FileUtil.zipUnZipFile(inZipFile);
            //解压input文件
            if (FileUtil.unZip(inZipFile)){
                //进入解压后的文件夹
                String unInZipDirName = inZipFile.getName().substring(0, inZipFile.getName().indexOf("."));
                unInZipFileDir = new File(inZipFile.getParent(), unInZipDirName);
                File tempFile = null;
                for (File unInZipFile : unInZipFileDir.listFiles()){
                    if (unInZipFile.getName().startsWith("JY") && unInZipFile.length() > 0){
                        tempFile = unInZipFile;
                        if ("02".equals(zipFileType)){  //消费文件
                            isBusFile = FileUtil.isBusFile(unInZipDirName, unInZipFile);
                            if (isBusFile){
                                resultMap.put("isBusFile","yes");
                            }else {
                                resultMap.put("isBusFile","no");
                            }
                        }
                        break;
                    }
                }
                if (tempFile != null){
                    boolean insertFlag = false;
                    resultMap.put("consumeJyIsNull","no");
                    if ("02".equals(zipFileType)){  //消费
                        //获取output对应的文件夹
                        File outputDateDir = new File(outputDataFolder + File.separator + date);
                        boolean outputZipFileExists = false;
                        for (File outputZipFile : outputDateDir.listFiles()){
                            if (outputZipFile.getName().startsWith(unInZipFileDir.getName())){
                                outputZipFileExists = true;
                                //处理已经解压过的文件夹
                                outputZipFile = FileUtil.zipUnZipFile(outputZipFile);
                                //解压
                                if (FileUtil.unZip(outputZipFile)){
                                    unOutZipFileDir = new File(outputDateDir,outputZipFile.getName().substring(0,outputZipFile.getName().indexOf(".")));
                                }else {
                                    log.error("解压output的文件{}失败,修改标志，通知其他线程", outputZipFile.getAbsolutePath());
                                    //删除解压的文件夹
                                    FileUtil.deleteFile(unInZipFileDir);
                                    FileUtil.deleteFile(unOutZipFileDir);
                                    //修改
                                    processResultService.update(
                                            new FileProcessResult(outputZipFile.getName(), null, new Date(),
                                                    "6555", "解压output文件失败"));
                                    return false;
                                }
                            }
                        }
                        if (!outputZipFileExists){
                            resultMap.put("consumeJyIsNull","yes");
                            //修改
                            processResultService.update(
                                    new FileProcessResult(inZipFile.getName(), null, new Date(),
                                            "6555", "对应的output文件不存在"));
                            //删除解压的文件夹
                            FileUtil.deleteFile(unInZipFileDir);
                            return true;
                        }

                        //落库
                        insertFlag = batchInsert(date,unOutZipFileDir, inZipFile.getName(), tempFile, isBusFile, sqlldrDir,
                                                        dbUser,dbPassword,odbName);
                    }else  if ("03".equals(zipFileType)) { //客服
                        insertFlag = customerServiceDataProcess.batchInsert(date,inZipFile.getName(), tempFile, sqlldrDir,
                                                                                            dbUser,dbPassword,odbName);
                    }
                    FileUtil.deleteFile(unInZipFileDir);
                    FileUtil.deleteFile(unOutZipFileDir);
                    return insertFlag;
                }else {
                    log.error("{}没有JY文件,无需处理。",inZipFile.getAbsolutePath());
                    resultMap.put("consumeJyIsNull","yes");
                    FileUtil.deleteFile(unInZipFileDir);
                    FileUtil.deleteFile(unOutZipFileDir);
                    //修改
                    processResultService.update(
                            new FileProcessResult(inZipFile.getName(), null, new Date(),
                                    "0000", "input的压缩文件里的JY文件没有数据，无需处理。"));
                    return true;
                }
            }else{
                log.error("解压input文件{}失败,修改标志，通知其他线程", inZipFile.getAbsolutePath());
                //删除解压的文件夹
                FileUtil.deleteFile(unInZipFileDir);
                //修改
                processResultService.update(
                        new FileProcessResult(inZipFile.getName(), null, new Date(),
                                "6555", "解压input文件失败"));
                return false;
            }

        }catch (Exception e){
            log.error("处理消费文件{}发生异常：{},修改标志，通知其他线程", inZipFile.getAbsolutePath(), e);
            //删除解压的文件夹
            FileUtil.deleteFile(unInZipFileDir);
            FileUtil.deleteFile(unOutZipFileDir);
            //修改
            processResultService.update(new FileProcessResult(inZipFile.getName(),null,new Date(),
                    "6555","处理消费或者客服文件发生异常"));
            return false;
        }
    }

    private boolean auditAndCount(String inZipFileName, boolean isBusFile, String date, Map<String, String> resultMap) {
        long investNotes = 0L;
        BigDecimal investAmount = new BigDecimal("0");
        long consumeNotes = 0L;
        BigDecimal consumeAmount = new BigDecimal("0");
        long reviseNotes = 0L;
        BigDecimal reviseAmount = new BigDecimal("0");
        String resultCode = null;
        String resultMsg = null;
        String consumeProcessNextZipFile = null;
        try {
            if (inZipFileName.startsWith("CX")) {  //cpu消费文件
                if (isBusFile){ //公交
                    log.info("cpu卡公交消费文件内容校验");
                    long cpuCwNotes = cpuConsumeMapper.findCwNotes(date,inZipFileName);
                    long cwNotes = cpuConsumeMapper.countCwNotes(date,inZipFileName);
                    //校验错误笔数
                    if (cpuCwNotes == cwNotes){
                        consumeProcessNextZipFile = "no";
                        //校验清算金额
                        BigDecimal qsTotalAmount = settleAuditMapper.countTotalAmount(date,inZipFileName);
                        if (qsTotalAmount == null){
                            qsTotalAmount = new BigDecimal("0");
                        }
                        CountData cpuConsumeCountData = cpuConsumeMapper.countAmountAndNum(date,inZipFileName);
                        if (cpuConsumeCountData != null && cpuConsumeCountData.getAmountSum() == null){
                            cpuConsumeCountData.setAmountSum(new BigDecimal("0"));
                        }
                        if (qsTotalAmount.compareTo(cpuConsumeCountData.getAmountSum()) == 0){
                            resultCode = "0000";
                            resultMsg = "处理成功";
                            log.info("校验成功。cpu卡公交消费文件统计并落库");
                        }else {
                            resultCode = "0030";
                            resultMsg = "cpu卡公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                            log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,cpuConsumeCountData.getAmountSum());
                            fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                    resultMsg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                   cpuConsumeCountData.getAmountSum(),qsTotalAmount));
                        }
                        consumeNotes = cpuConsumeCountData.getNotesSum();
                        consumeAmount = cpuConsumeCountData.getAmountSum();
                        CountData cpuConsumeReviseCountData = cpuConsumeReviseMapper.countAmountAndNum(date,inZipFileName);
                        reviseNotes = cpuConsumeReviseCountData.getNotesSum();
                        reviseAmount = cpuConsumeReviseCountData.getAmountSum();
                    }else {
                        consumeProcessNextZipFile = "yes";
                        resultCode = "6555";
                       resultMsg = "cpu卡公交消费文件错误文件的笔数与消费文件错误笔数不符";
                        log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,cpuCwNotes);
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                resultMsg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                null,null));
                    }
                }else { //cpu卡非公交
                    log.info("cpu卡非公交消费文件内容校验");
                    long cpuCwNotes = cpuConsumeNoBusMapper.findCwNotes(date,inZipFileName);
                    long cwNotes = cpuConsumeNoBusMapper.countCwNotes(date,inZipFileName);
                    //校验错误笔数
                    if (cpuCwNotes == cwNotes) {
                        consumeProcessNextZipFile = "no";
                        //校验清算金额
                        BigDecimal qsTotalAmount = settleAuditMapper.countTotalAmount(date, inZipFileName);
                        if (qsTotalAmount == null){
                            qsTotalAmount = new BigDecimal("0");
                        }
                        CountData cpuConsumeCountData = cpuConsumeNoBusMapper.countAmountAndNum(date, inZipFileName);
                        if (cpuConsumeCountData != null && cpuConsumeCountData.getAmountSum() == null){
                            cpuConsumeCountData.setAmountSum(new BigDecimal("0"));
                        }
                        if (qsTotalAmount.compareTo(cpuConsumeCountData.getAmountSum()) == 0) {
                            resultCode = "0000";
                            resultMsg = "处理成功";
                            log.info("校验成功。cpu卡非公交消费文件统计并落库");
                        }else {
                            resultCode = "0030";
                            resultMsg = "cpu卡非公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                            log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,cpuConsumeCountData.getAmountSum());
                            fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                    resultMsg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                    cpuConsumeCountData.getAmountSum(),qsTotalAmount));
                        }
                        consumeNotes = cpuConsumeCountData.getNotesSum();
                        consumeAmount = cpuConsumeCountData.getAmountSum();
                    }else {
                        consumeProcessNextZipFile = "yes";
                        resultCode = "6555";
                        resultMsg = "cpu卡非公交消费文件错误文件的笔数与消费文件错误笔数不符";
                        log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,cpuCwNotes);
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                resultMsg,0L,null,0L,null,cpuCwNotes,cwNotes,
                                null,null));
                    }
                }
            }else if (inZipFileName.startsWith("XF")){  //m1卡
                if (isBusFile){
                    log.info("m1卡公交消费文件内容校验");
                    long mCardCwNotes = mCardConsumeMapper.findCwNotes(date, inZipFileName);
                    long cwNotes = mCardConsumeMapper.countCwNotes(date, inZipFileName);
                    //校验错误笔数
                    if (mCardCwNotes == cwNotes) {
                        consumeProcessNextZipFile = "no";
                        //校验清算金额
                        BigDecimal qsTotalAmount = null;
                        qsTotalAmount = settleAuditMapper.countTotalAmount(date, inZipFileName);
                        if (qsTotalAmount == null){
                            qsTotalAmount = new BigDecimal("0");
                        }
                        CountData mConsumeCountData = mCardConsumeMapper.countAmountAndNum(date, inZipFileName);
                        if (mConsumeCountData != null && mConsumeCountData.getAmountSum() == null) {
                            mConsumeCountData.setAmountSum(new BigDecimal("0"));
                        }
                        if (qsTotalAmount.compareTo(mConsumeCountData.getAmountSum()) == 0) {
                            resultCode = "0000";
                            resultMsg = "处理成功";
                            log.info("校验成功.m1卡公交消费文件统计数据并落库");
                        }else {
                            resultCode = "0030";
                            resultMsg = "m1卡公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                            log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,mConsumeCountData.getAmountSum());
                            fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                    resultMsg,0L,null,0L,null,mCardCwNotes,cwNotes,
                                   mConsumeCountData.getAmountSum(),qsTotalAmount));
                        }
                        consumeNotes = mConsumeCountData.getNotesSum();
                        consumeAmount = mConsumeCountData.getAmountSum();
                        CountData cpuConsumeRevise = mCardConsumeReviseMapper.countAmountAndNum(date, inZipFileName);
                        reviseNotes = cpuConsumeRevise.getNotesSum();
                        reviseAmount = cpuConsumeRevise.getAmountSum();
                    }else {
                        consumeProcessNextZipFile = "yes";
                        resultCode = "6555";
                        resultMsg = "m1卡公交消费文件错误文件的笔数与消费文件错误笔数不符";
                        log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,mCardCwNotes);
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                resultMsg,0L,null,0L,null,mCardCwNotes,cwNotes,
                                null,null));
                    }
                }else {
                    log.info("m1卡非公交消费文件内容校验");
                    long mCardCwNotes = mCardConsumeNoBusMapper.findCwNotes(date, inZipFileName);
                    long cwNotes = mCardConsumeNoBusMapper.countCwNotes(date, inZipFileName);
                    //校验错误笔数
                    if (mCardCwNotes == cwNotes) {
                        consumeProcessNextZipFile = "no";
                        //校验清算金额
                        BigDecimal qsTotalAmount = settleAuditMapper.countTotalAmount(date, inZipFileName);
                        if (qsTotalAmount == null){
                            qsTotalAmount = new BigDecimal("0");
                        }
                        CountData mCardConsumeCountData = mCardConsumeNoBusMapper.countAmountAndNum(date, inZipFileName);
                        if (mCardConsumeCountData != null && mCardConsumeCountData.getAmountSum() == null) {
                            mCardConsumeCountData.setAmountSum(new BigDecimal("0"));
                        }
                        if (qsTotalAmount.compareTo(mCardConsumeCountData.getAmountSum()) == 0) {
                            resultCode = "0000";
                            resultMsg = "处理成功";
                            log.info("校验成功。m1卡非公交消费文件统计数据并落库");
                        }else {
                            resultCode = "0030";
                            resultMsg = "m1卡非公交消费文件清算文件的金额与消费文件金额减去错误文件金额不符";
                            log.error("{}校验失败，清算文件的金额:{},消费文件金额减去错误文件金额:{}。",inZipFileName,qsTotalAmount,mCardConsumeCountData.getAmountSum());
                            fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                    resultMsg,0L,null,0L,null,mCardCwNotes,cwNotes,
                                    mCardConsumeCountData.getAmountSum(),qsTotalAmount));
                            //记录异常交易
                            processResultService.delAndInsert(date,inZipFileName);
                        }
                        consumeNotes = mCardConsumeCountData.getNotesSum();
                        consumeAmount = mCardConsumeCountData.getAmountSum();
                    }else {
                        consumeProcessNextZipFile = "yes";
                        resultCode = "6555";
                        resultMsg = "m1卡非公交消费文件错误文件的笔数与消费文件错误笔数不符";
                        log.error("{}校验失败，错误文件的笔数{},消费文件错误笔数:{}。",inZipFileName,cwNotes,mCardCwNotes);
                        fileCheckErrorMapper.insert(new FileCheckError(date,inZipFileName,"02","01",new Date(),"6555",
                                resultMsg,0L,null,0L,null,mCardCwNotes,cwNotes,
                                null,null));
                    }
                }
            }else if (inZipFileName.startsWith("Ck")){ //cpu卡客服
                log.info("cpu卡客服文件统计数据并落库");
                resultCode = "0000";
                resultMsg = "处理成功";
                //cpu客服充值
                CountData cpuInvest = cpuCustomerServiceMapper.countInvestAmountAndNum(date, inZipFileName);
                CountData cpuConsume = cpuCustomerServiceMapper.countConsumeAmountAndNum(date, inZipFileName);
                investNotes = cpuInvest.getNotesSum();
                investAmount = cpuInvest.getAmountSum();
                consumeNotes = cpuConsume.getNotesSum() ;
                consumeAmount = cpuConsume.getAmountSum();
            }else if (inZipFileName.startsWith("KF")){ //m1卡客服
                log.info("m1卡客服文件统计数据并落库");
                resultCode = "0000";
                resultMsg = "处理成功";
                //m1客服充值
                CountData mCardInvest = mCardCustomerServiceMapper.countInvestAmountAndNum(date, inZipFileName);
                CountData mCardConsume = mCardCustomerServiceMapper.countConsumeAmountAndNum(date, inZipFileName);
                investNotes = mCardInvest.getNotesSum();
                investAmount =mCardInvest.getAmountSum();
                consumeNotes = mCardConsume.getNotesSum();
                consumeAmount = mCardConsume.getAmountSum();
            }
            processResultService.update(
                    new FileProcessResult(inZipFileName,new Date(),resultCode,resultMsg, investNotes,investAmount,
                            consumeNotes,consumeAmount,reviseNotes,reviseAmount));
            resultMap.put("consumeProcessNextZipFile",consumeProcessNextZipFile );
            return true;
        }catch (Exception e){
            log.error("统计消费或者客服文件{}的笔数和金额发生异常:{}。",inZipFileName,e);
            //修改
            processResultService.update(
                    new FileProcessResult(inZipFileName, null, new Date(),
                            "6555", "统计消费或者客服文件的笔数和金额发生异常"));
            return false;
        }
    }

    private boolean writerConsumeOrCustomerToDm(File dmmj, File dmmx, File dmcj, File dmcx, String date,
                                             String inZipFileName, Boolean isBusFile,String zipFileType) {
        boolean flag = false;
        if ("02".equals(zipFileType)){  //消费
            //从数据库取出写入dm
            flag = writerTODM(dmmj,dmmx,dmcj,dmcx,date,inZipFileName,isBusFile);
        }else if ("03".equals(zipFileType)){ //客服
            //从数据库取出写入dm
            flag = customerServiceDataProcess.writerTODM(dmmj, dmcj, date, inZipFileName);
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
    public boolean batchInsert(String date,File unOutZipFileDir, String inZipFileName, File unZipFile, Boolean isBusFile,
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
                        if (inZipFileName.startsWith("XF0000") || inZipFileName.startsWith("XF0002")) {
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
    public boolean writerTODM(File dmmj, File dmmx, File dmcj, File dmcx,
                                    String settleDate, String zipFileName, Boolean isBusFile) {
        try {
            log.info("从数据库取消费数据写到对应的dm文件");
            if (zipFileName.startsWith("CX")){ //cpu卡
                ArrayList<CpuTrade> cpuTradeList = new ArrayList<>();
                if (isBusFile){ //公交
                    //获取总笔数
                    long allNotes = cpuConsumeMapper.findAllNotes(settleDate,zipFileName);
                    if (allNotes > 0L) {
                        long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                        while (allNotes > 0L) {
                            //其他线程检查
                            if (threadTaskHandle.getIsError()) {
                                log.error("有线程发生了异常，无需再执行！");
                                return false;
                            }
                            if (allNotes <= pageSize) {
                                pageSize = allNotes;
                            }
                            endNum = startNum + pageSize;
                            log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                            List<CpuConsume> cpuConsumeList = cpuConsumeMapper.findByWhere(startNum,endNum,settleDate,zipFileName);
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
                    List<CpuConsumeRevise> list = cpuConsumeReviseMapper.findList(settleDate,zipFileName);
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
                    long allNotes = cpuConsumeNoBusMapper.findAllNotes(settleDate,zipFileName);
                    if (allNotes > 0L) {
                        long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                        while (allNotes > 0L) {
                            //其他线程检查
                            if (threadTaskHandle.getIsError()) {
                                log.error("有线程发生了异常，无需再执行！");
                                return false;
                            }
                            if (allNotes <= pageSize) {
                                pageSize = allNotes;
                            }
                            endNum = startNum + pageSize;
                            log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                            List<CpuConsumeNoBus> cpuConsumeNoBusList = cpuConsumeNoBusMapper.findByWhere(startNum,endNum,settleDate,zipFileName);
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
                    long allNotes = mCardConsumeMapper.findAllNotes(settleDate,zipFileName);
                    if (allNotes > 0L){
                        long pageSize = 100000L,startNum = 0L,endNum,count = 1L;
                        while (allNotes > 0L){
                            //其他线程检查
                            if (threadTaskHandle.getIsError()) {
                                log.error("有线程发生了异常，无需再执行！");
                                return false;
                            }
                            if (allNotes <= pageSize){
                                pageSize = allNotes;
                            }
                            endNum = startNum + pageSize;
                            log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}",count,allNotes,pageSize,startNum,endNum);
                            List<MCardConsume> mCardConsumeList = mCardConsumeMapper.findByWhere(startNum,endNum,settleDate,zipFileName);
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
                    List<MCardConsumeRevise> reviseList = mCardConsumeReviseMapper.findList(settleDate,zipFileName);
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
                    long allNotes = mCardConsumeNoBusMapper.findAllNotes(settleDate,zipFileName);
                    if (allNotes > 0L) {
                        long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                        while (allNotes > 0L) {
                            //其他线程检查
                            if (threadTaskHandle.getIsError()) {
                                log.error("有线程发生了异常，无需再执行！");
                                return false;
                            }
                            if (allNotes <= pageSize) {
                                pageSize = allNotes;
                            }
                            endNum = startNum + pageSize;
                            log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                            List<MCardConsumeNoBus> list = mCardConsumeNoBusMapper.findByWhere(startNum,endNum,settleDate,zipFileName);
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
            return true;
        }catch (Exception e){
            log.error("把消费文件{}写到dm文件发生异常:{}。",zipFileName,e);
            //修改
            processResultService.update(
                    new FileProcessResult(zipFileName, null, new Date(),
                            "6555", "把消费文件写到dm文件发生异常"));
            return false;
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
