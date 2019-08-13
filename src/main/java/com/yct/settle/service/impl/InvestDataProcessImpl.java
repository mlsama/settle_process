package com.yct.settle.service.impl;

import com.yct.settle.mapper.*;
import com.yct.settle.pojo.*;
import com.yct.settle.service.AreaService;
import com.yct.settle.service.BatchInsertService;
import com.yct.settle.service.InvestDataProcess;
import com.yct.settle.thread.ThreadTaskHandle;
import com.yct.settle.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DESC: cpu卡的充值数据服务类
 * AUTHOR:mlsama
 * 2019/6/28 9:46
 */
@Service
@Slf4j
public class InvestDataProcessImpl implements InvestDataProcess {
    @Resource
    private ProcessResultServiceImpl processResultService;
    @Resource
    private CpuInvestMapper cpuInvestMapper;
    @Resource
    private CpuInvestReviseHisMapper cpuInvestReviseHisMapper;
    @Resource
    private CpuInvestCheckBackMapper cpuInvestCheckBackMapper;
    @Resource
    private CpuInvestCheckBackHisMapper cpuInvestCheckBackHisMapper;
    @Resource
    private MCardInvestMapper mCardInvestMapper;
    @Resource
    private MCardInvestCheckBackMapper mCardInvestCheckBackMapper;
    @Resource
    private MCardInvestReviseHisMapper mCardInvestReviseHisMapper;
    @Resource
    private MCardInvestCheckBackHisMapper mCardInvestCheckBackHisMapper;
    @Resource
    private ThreadTaskHandle threadTaskHandle;
    @Resource
    private AreaService areaService;
    @Resource
    private BatchInsertService batchInsertService;

    /**
     * @param inputDataFolder  input文件夹路径
     * @param outputDataFolder output文件夹路径
     * @param date             结算日期
     * @param dmcj             cpu交易明细文件
     * @param dmcx             CPU修正明细文件
     * @param dmmj             m1交易明细文件
     * @param dmmx             m1修正明细文件
     * @param dbUser           数据库用户名
     * @param dbPassword       数据库密码
     * @param odbName          数据库名称
     * @param resultMap        返回结果容器
     * @return
     */
    @Override
    public boolean processInvestFiles(String inputDataFolder,String outputDataFolder, String date, File dmcj,
                                      File dmcx, File dmmj, File dmmx, String dbUser,
                                      String dbPassword, String odbName, File sqlldrDir, Map<String, String> resultMap) {
        File outputDateDir = new File(outputDataFolder + File.separator + date);
        log.info("开始处理文件夹{}下的充值文件", outputDateDir.getName());
        String fileName = null;
        try {
            if (outputDateDir.exists()) {
                for (File outZipFile : outputDateDir.listFiles()) {
                    //其他线程检查
                    if (threadTaskHandle.getIsError()) {
                        log.error("有线程发生了异常，处理充值文件的线程无需再执行！");
                        return false;
                    }
                    //压缩包文件名称
                    fileName = outZipFile.getName();
                    if (fileName.startsWith("CC") || fileName.startsWith("CZ")) {
                        //落库文件处理表
                        FileProcessResult result = new FileProcessResult();
                        result.setSettleDate(date);
                        result.setZipFileName(fileName);
                        result.setZipFileType("01");
                        if (fileName.startsWith("CC")) {
                            result.setCardType("01");
                        } else {
                            result.setCardType("02");
                        }
                        processResultService.delAndInsert(result);
                        //压缩文件校验并落库
                        boolean insertFlag = auditAndInsert(outZipFile,inputDataFolder,date,dbUser, dbPassword, odbName, sqlldrDir,resultMap);
                        if (!insertFlag) {
                            threadTaskHandle.setIsError(true);
                            return false;
                        }
                        if ("yes".equals(resultMap.get("investProcessNextZipFile"))){
                            continue;
                        }
                        //从数据库取出数据写入dm
                        boolean writerToDmFlag = writerToDm(fileName, date, dmmj, dmmx, dmcj, dmcx);
                        if (!writerToDmFlag){
                            threadTaskHandle.setIsError(true);
                            return false;
                        }
                        //把这个充值文件的统计数据存到数据库表
                        boolean countInvestDataFlag = countInvestDataToDb(date,fileName);
                        if (!countInvestDataFlag){
                            threadTaskHandle.setIsError(true);
                            return false;
                        }
                    }
                }
                resultMap.put("investResultCode", "0000");
            } else {
                log.error("文件夹{}不存在", outputDateDir.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            threadTaskHandle.setIsError(true);
            log.error("处理充值文件{}发生异常：{},修改标志，通知其他线程", fileName, e);
            //修改
            processResultService.update(new FileProcessResult(fileName, null, new Date(), "6555", "处理充值文件发生异常"));
            return false;
        }
        return true;
    }

    /**
     * 批量导入充值数据
     * @param outZipFile        output压缩充值文件
     * @param inputDataFolder   input文件夹
     * @param date              清算日期
     * @param dbUser            数据库用户名
     * @param dbPassword        密码
     * @param odbName           库名
     * @param sqlldrDir         sqlldr文件夹
     * @param resultMap
     */
    private boolean auditAndInsert(File outZipFile, String inputDataFolder, String date,
                                          String dbUser, String dbPassword, String odbName, File sqlldrDir, Map<String, String> resultMap) {
        File unOutZipFileDir = null;  //output解压后的文件夹
        File unInZipFileDir = null;  //input解压后的文件夹
        try {
            //处理已经解压过的文件夹
            outZipFile = FileUtil.zipUnZipFile(outZipFile);
            if (FileUtil.unZip(outZipFile)) {   //output压缩文件解压
                unOutZipFileDir = new File(outZipFile.getParent(), outZipFile.getName().substring(0, outZipFile.getName().indexOf(".")));
                //找到对应input的JY
                File inputDateDir = new File(inputDataFolder + File.separator + date);
                File targetFile = null;
                File tempFile = null;
                for (File inZipFile : inputDateDir.listFiles()) {
                    if (inZipFile.getName().startsWith(unOutZipFileDir.getName())) {
                        tempFile = inZipFile;
                        //处理已经解压过的文件夹
                        inZipFile = FileUtil.zipUnZipFile(inZipFile);
                        //解压input文件
                        if (FileUtil.unZip(inZipFile)) {
                            //进入解压目录
                            unInZipFileDir = new File(inputDateDir, inZipFile.getName().substring(0, inZipFile.getName().indexOf(".")));
                            for (File inputFile : unInZipFileDir.listFiles()) {
                                if (inputFile.getName().startsWith("JY") && inputFile.length() > 0) {
                                    targetFile = inputFile;
                                    break;
                                }
                            }
                            break;
                        } else {
                            log.error("解压input文件{}失败,修改标志，通知其他线程", outZipFile.getAbsolutePath());
                            //删除解压的文件夹
                            FileUtil.deleteFile(unOutZipFileDir);
                            FileUtil.deleteFile(unInZipFileDir);
                            //修改
                            processResultService.update(
                                    new FileProcessResult(outZipFile.getName(), null, new Date(),
                                            "6555", "解压input文件失败"));
                            return false;
                        }
                    }
                }

                //检查output的JY是否为空
                File jy = null;
                for (File unOutZipFile : unOutZipFileDir.listFiles()) {
                    if (unOutZipFile.getName().startsWith("JY") && unOutZipFile.length() > 0) {
                        jy = unOutZipFile;
                    }
                }
                boolean checkJy = false;
                String code = null;
                String msg = null;
                resultMap.put("investProcessNextZipFile","no");
                if (jy == null){
                    checkJy = true;
                    if (targetFile == null){
                        resultMap.put("investProcessNextZipFile","yes");
                        code = "0000";
                        msg = "input,output都没有JY文件或内容为空，无需处理";
                        //修改
                        processResultService.update(
                                new FileProcessResult(outZipFile.getName(), null, new Date(),code, msg));
                        FileUtil.deleteFile(unOutZipFileDir);
                        FileUtil.deleteFile(unInZipFileDir);
                        return true;
                    }else {
                        code = "6555";
                        msg = "output没有JY文件或内容为空，但input的JY有数据";
                        //把input的JY复制到output
                        FileUtil.copyJyInToOut(targetFile,unOutZipFileDir);
                    }
                    log.info(msg);
                }else {
                    if (tempFile == null) { //input下没有一样的压缩文件
                        checkJy = true;
                        code = "6555";
                        msg = "output的{}的JY文件有数据，input下没有对应的压缩文件";
                        log.info(msg);
                    }
                    if (targetFile == null){
                        checkJy = true;
                        code = "6555";
                        msg = "output的JY文件有数据，input压缩文件里没有JY文件";
                        log.info(msg);
                    }
                }
                if (checkJy){
                    String cardType = outZipFile.getName().startsWith("CC") ? "01" : "02";
                    //插入检查错误表
                    processResultService.delAndInsert(
                            new FileCheckError(date,outZipFile.getName(),"01",cardType,new Date(),code,msg));
                }
                //落库，校验
                for (File unOutZipFile : unOutZipFileDir.listFiles()) {
                    if (!(unOutZipFile.getName().startsWith("MD") || unOutZipFile.getName().startsWith("QS") ||
                            unOutZipFile.getName().startsWith("RZ"))){
                        boolean insertFlag = batchInsertService.batchInsertInvestData(date, unOutZipFile, outZipFile.getName(), dbUser,
                                dbPassword, odbName, sqlldrDir, targetFile,resultMap);
                        if (!insertFlag) {
                            FileUtil.deleteFile(unOutZipFileDir);
                            FileUtil.deleteFile(unInZipFileDir);
                            return false;
                        }
                    }
                }
                FileUtil.deleteFile(unOutZipFileDir);
                FileUtil.deleteFile(unInZipFileDir);
                return true;
            }else {
                log.error("解压output文件{}失败,修改标志，通知其他线程", outZipFile.getAbsolutePath());
                //删除解压的文件夹
                FileUtil.deleteFile(unOutZipFileDir);
                //修改
                processResultService.update(
                        new FileProcessResult(outZipFile.getName(), null, new Date(),
                                                "6555", "解压output文件失败"));
                return false;
            }
        }catch (Exception e){
            log.error("处理文件{}发生异常,修改标志，通知其他线程。case by :{}", outZipFile.getAbsolutePath(),e);
            //删除解压的文件夹
            FileUtil.deleteFile(unOutZipFileDir);
            FileUtil.deleteFile(unInZipFileDir);
            //修改
            processResultService.update(
                    new FileProcessResult(outZipFile.getName(), null, new Date(),
                            "6555", "处理文件发生异常"));
            return false;
        }
    }



    /**
     * 统计并落库
     * @param date
     * @param zipFileName
     */
    private boolean countInvestDataToDb(String date, String zipFileName) {
        try {
            long investNotes = 0L;
            BigDecimal investAmount = new BigDecimal("0");
            long reviseNotes = 0L;
            BigDecimal reviseAmount  = new BigDecimal("0") ;
            if (zipFileName.startsWith("CC")){
                log.info("统计cpu卡充值,修正的笔数和金额");
                CountData investCountData = addContDate(cpuInvestMapper.countData(date,zipFileName), cpuInvestCheckBackMapper.countData(date,zipFileName));
                investNotes = investCountData.getNotesSum();
                investAmount = investCountData.getAmountSum();

                CountData reviseCountData = addContDate(cpuInvestCheckBackHisMapper.countData(date,zipFileName), cpuInvestReviseHisMapper.countData(date,zipFileName));
                reviseNotes = reviseCountData.getNotesSum();
                reviseAmount = reviseCountData.getAmountSum();
            }else {
                log.info("统计m1卡充值，修正的笔数和金额");
                CountData investCountData = addContDate(mCardInvestMapper.countData(date,zipFileName), mCardInvestCheckBackMapper.countData(date,zipFileName));
                investNotes = investCountData.getNotesSum();
                investAmount = investCountData.getAmountSum();

                CountData reviseCountData = addContDate(mCardInvestCheckBackHisMapper.countData(date,zipFileName), mCardInvestReviseHisMapper.countData(date,zipFileName));
                reviseNotes = reviseCountData.getNotesSum();
                reviseAmount = reviseCountData.getAmountSum();
            }
            processResultService.update(
                    new FileProcessResult(zipFileName,new Date(),"0000","处理成功", investNotes,investAmount,
                            0L,new BigDecimal("0"),reviseNotes,reviseAmount));
            return true;
        }catch (Exception e){
            log.error("统计充值文件{}的笔数和金额发生异常。",zipFileName);
            //修改
            processResultService.update(
                    new FileProcessResult(zipFileName, null, new Date(),
                            "6555", "统计充值文件的笔数和金额发生异常"));
            return false;
        }
    }

    private CountData addContDate(CountData v1,CountData v2){
        v1.setNotesSum(MathUtil.longAdd(v1.getNotesSum(),v2.getNotesSum()));
        v1.setAmountSum(AmountUtil.add(v1.getAmountSum(),v2.getAmountSum()));
        return v1;
    }

    private boolean writerToDm(String zipFileName, String settleDate, File dmmj, File dmmx, File dmcj, File dmcx) {
        try {
            log.info("从数据库取充值数据写到对应的dm文件");
            //根据服务商代码确定卡使用地：USEA
            String userArea = areaService.getUseAreaByMerchant(zipFileName);
            if (zipFileName.startsWith("CC")){ //cpu卡
                ArrayList<CpuTrade> cpuTradeList = new ArrayList<>();
                //获取总笔数
                long allNotes = cpuInvestMapper.findAllNotes(settleDate,zipFileName);
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
                        List<CpuInvest> cpuInvestList = cpuInvestMapper.findByWhere(startNum,endNum,settleDate,zipFileName);
                        if (cpuInvestList.size() > 0){
                            for (CpuInvest cpuInvest : cpuInvestList){
                                CpuTrade cpuTrade = new CpuTrade();
                                convertToCpuTrade(cpuInvest,cpuTrade,settleDate,zipFileName,userArea);
                                cpuTradeList.add(cpuTrade);
                            }
                        }
                        FileUtil.writeToFile(dmcj, cpuTradeList);
                        cpuTradeList.clear();
                        allNotes -= pageSize;
                        startNum += pageSize;
                        count++;
                    }
                }
                List<CpuInvestCheckBack> cpuInvestCheckBackList = cpuInvestCheckBackMapper.findByWhere(settleDate,zipFileName);
                if (cpuInvestCheckBackList.size() > 0){
                    for (CpuInvestCheckBack cpuInvestCheckBack : cpuInvestCheckBackList){
                        CpuTrade cpuTrade = new CpuTrade();
                        convertToCpuTrade(cpuInvestCheckBack,cpuTrade,settleDate,zipFileName,userArea);
                        cpuTradeList.add(cpuTrade);
                    }
                }
                FileUtil.writeToFile(dmcj, cpuTradeList);
                //修正
                ArrayList<CpuTradeRevise> cpuTradeReviseList = new ArrayList<>();
                List<CpuInvestReviseHis> cpuInvestReviseHisList = cpuInvestReviseHisMapper.findList(settleDate,zipFileName);
                if (cpuInvestReviseHisList.size() > 0){
                    for (CpuInvestReviseHis cpuInvestReviseHis : cpuInvestReviseHisList){
                        CpuTradeRevise cpuTradeRevise = new CpuTradeRevise();
                        convertToCpuTradeRevise(cpuInvestReviseHis,cpuTradeRevise,settleDate,zipFileName,userArea);
                        cpuTradeReviseList.add(cpuTradeRevise);
                    }
                }
                List<CpuInvestCheckBackHis> investCheckBackHisList = cpuInvestCheckBackHisMapper.findList(settleDate,zipFileName);
                if (investCheckBackHisList.size() > 0){
                    for (CpuInvestCheckBackHis cpuInvestCheckBackHis : investCheckBackHisList){
                        CpuTradeRevise cpuTradeRevise = new CpuTradeRevise();
                        convertToCpuTradeRevise(cpuInvestCheckBackHis,cpuTradeRevise,settleDate,zipFileName,userArea);
                        cpuTradeReviseList.add(cpuTradeRevise);
                    }
                }
                FileUtil.writeToFile(dmcx, cpuTradeReviseList);

            }else { //m1卡
                ArrayList<MCardTrade> mCardTradeList = new ArrayList<>();
                //获取总笔数
                long allNotes = mCardInvestMapper.findAllNotes(settleDate,zipFileName);
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
                        List<MCardInvest> mCardInvestList = mCardInvestMapper.findByWhere(startNum,endNum,settleDate,zipFileName);
                        if (mCardInvestList.size() > 0){
                            for (MCardInvest mCardInvest : mCardInvestList){
                                MCardTrade mCardTrade = new MCardTrade();
                                convertToMCardTrade(mCardInvest,mCardTrade,settleDate,zipFileName,userArea);
                                mCardTradeList.add(mCardTrade);
                            }
                        }
                        FileUtil.writeToFile(dmmj, mCardTradeList);
                        mCardTradeList.clear();
                        allNotes -= pageSize;
                        startNum += pageSize;
                        count++;
                    }
                }

                List<MCardInvestCheckBack> mCardInvestCheckBackList = mCardInvestCheckBackMapper.findByWhere(settleDate,zipFileName);
                if (mCardInvestCheckBackList.size() > 0){
                    for (MCardInvestCheckBack mCardInvestCheckBack : mCardInvestCheckBackList){
                        MCardTrade mCardTrade = new MCardTrade();
                        convertToMCardTrade(mCardInvestCheckBack,mCardTrade,settleDate,zipFileName,userArea);
                        mCardTradeList.add(mCardTrade);
                    }
                }
                FileUtil.writeToFile(dmmj, mCardTradeList);

                //修正
                ArrayList<MCardTradeRevise> mCardTradeReviseList = new ArrayList<>();
                List<MCardInvestReviseHis> mCardInvestReviseHisList = mCardInvestReviseHisMapper.findList(settleDate,zipFileName);
                if (mCardInvestReviseHisList.size() > 0){
                    for (MCardInvestReviseHis mCardInvestReviseHis : mCardInvestReviseHisList){
                        MCardTradeRevise mCardTradeRevise = new MCardTradeRevise();
                        convertToMCardTradeRevise(mCardInvestReviseHis,mCardTradeRevise,settleDate,zipFileName,userArea);
                        mCardTradeReviseList.add(mCardTradeRevise);
                    }
                }
                List<MCardInvestCheckBackHis> mCardInvestCheckBackHisList = mCardInvestCheckBackHisMapper.findList(settleDate,zipFileName);
                if (mCardInvestCheckBackHisList.size() > 0){
                    for (MCardInvestCheckBackHis mCardInvestCheckBackHis : mCardInvestCheckBackHisList){
                        MCardTradeRevise mCardTradeRevise = new MCardTradeRevise();
                        convertToMCardTradeRevise(mCardInvestCheckBackHis,mCardTradeRevise,settleDate,zipFileName,userArea);
                        mCardTradeReviseList.add(mCardTradeRevise);
                    }
                }
                FileUtil.writeToFile(dmmx,mCardTradeReviseList);
            }
            return true;
        }catch (Exception e){
            log.error("把文件{}内容写到对应的dm文件发生异常:{}。",zipFileName,e);
            //修改
            processResultService.update(
                    new FileProcessResult(zipFileName, null, new Date(),
                            "6555", "统计充值文件的笔数和金额发生异常"));
            return false;
        }
    }

    private void convertToMCardTradeRevise(MCardInvestCheckBackHis mCardInvestCheckBackHis, MCardTradeRevise mCardTradeRevise,
                                           String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(mCardInvestCheckBackHis,mCardTradeRevise);
        mCardTradeRevise.setQDATE(settleDate);
        mCardTradeRevise.setQNAME(zipFileName);
        mCardTradeRevise.setLTIME(DateUtil.getTomorrow(mCardInvestCheckBackHis.getTIM()));
        mCardTradeRevise.setDT("01");
        mCardTradeRevise.setXT(mCardInvestCheckBackHis.getFLAG());
        mCardTradeRevise.setDSN(mCardInvestCheckBackHis.getPSN());
        mCardTradeRevise.setICN(mCardInvestCheckBackHis.getLCN());
        mCardTradeRevise.setFEE(new BigDecimal("0"));
        mCardTradeRevise.setBINF(mCardInvestCheckBackHis.getAPP()+"000000000000000000");
        mCardTradeRevise.setEPID("00000000");
        mCardTradeRevise.setETIM("00000000000000");
        mCardTradeRevise.setTAC("00000000");
        String issuea = areaService.getIssuesByCardNo(mCardInvestCheckBackHis.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        mCardTradeRevise.setUSEA(userArea);
        mCardTradeRevise.setISSUEA(issuea); //发行地
    }
    private void convertToMCardTradeRevise(MCardInvestReviseHis mCardInvestReviseHis, MCardTradeRevise mCardTradeRevise,
                                           String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(mCardInvestReviseHis,mCardTradeRevise);
        mCardTradeRevise.setQDATE(settleDate);
        mCardTradeRevise.setQNAME(zipFileName);
        mCardTradeRevise.setLTIME(DateUtil.getTomorrow(mCardInvestReviseHis.getTIM()));
        mCardTradeRevise.setDT("01");
        mCardTradeRevise.setXT(mCardInvestReviseHis.getFLAG());
        mCardTradeRevise.setDSN(mCardInvestReviseHis.getPSN());
        mCardTradeRevise.setICN(mCardInvestReviseHis.getLCN());
        mCardTradeRevise.setFEE(new BigDecimal("0"));
        mCardTradeRevise.setBINF(mCardInvestReviseHis.getAPP()+"000000000000000000");
        String issuea = areaService.getIssuesByCardNo(mCardInvestReviseHis.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        mCardTradeRevise.setUSEA(userArea);
        mCardTradeRevise.setISSUEA(issuea); //发行地
    }

    private void convertToMCardTrade(MCardInvestCheckBack mCardInvestCheckBack, MCardTrade mCardTrade,
                                     String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(mCardInvestCheckBack,mCardTrade);
        mCardTrade.setQDATE(settleDate);
        mCardTrade.setQNAME(zipFileName);
        mCardTrade.setDT("01");
        mCardTrade.setDSN(mCardInvestCheckBack.getPSN());
        mCardTrade.setICN(mCardInvestCheckBack.getLCN());
        mCardTrade.setFEE(new BigDecimal("0"));
        mCardTrade.setBINF(mCardInvestCheckBack.getAPP()+"000000000000000000");
        mCardTrade.setEPID("00000000");
        mCardTrade.setETIM("00000000000000");
        mCardTrade.setTAC("00000000");
        String issuea = areaService.getIssuesByCardNo(mCardInvestCheckBack.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        mCardTrade.setUSEA(userArea);
        mCardTrade.setISSUEA(issuea); //发行地
    }
    private void convertToMCardTrade(MCardInvest mCardInvest, MCardTrade mCardTrade, String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(mCardInvest,mCardTrade);
        mCardTrade.setQDATE(settleDate);
        mCardTrade.setQNAME(zipFileName);
        mCardTrade.setDT("01");
        mCardTrade.setDSN(mCardInvest.getPSN());
        mCardTrade.setICN(mCardInvest.getLCN());
        mCardTrade.setFEE(new BigDecimal("0"));
        mCardTrade.setBINF(mCardInvest.getAPP()+"000000000000000000");
        String issuea = areaService.getIssuesByCardNo(mCardInvest.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        mCardTrade.setUSEA(userArea);
        mCardTrade.setISSUEA(issuea); //发行地
    }

    private void convertToCpuTradeRevise(CpuInvestReviseHis cpuInvestReviseHis, CpuTradeRevise cpuTradeRevise,
                                                                    String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(cpuInvestReviseHis,cpuTradeRevise);
        cpuTradeRevise.setQDATE(settleDate);
        cpuTradeRevise.setLTIME(DateUtil.getTomorrow(cpuInvestReviseHis.getTIM()));
        cpuTradeRevise.setQNAME(zipFileName);
        cpuTradeRevise.setDT("01");
        cpuTradeRevise.setXT(cpuInvestReviseHis.getFLAG());
        cpuTradeRevise.setBINF(cpuInvestReviseHis.getAPP()+"000000000000000000");
        cpuTradeRevise.setDMON("0000000000000");
        String issuea = areaService.getIssuesByCardNo(cpuInvestReviseHis.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        cpuTradeRevise.setUSEA(userArea);
        cpuTradeRevise.setISSUEA(issuea); //发行地
    }
    private void convertToCpuTradeRevise(CpuInvestCheckBackHis cpuInvestCheckBackHis, CpuTradeRevise cpuTradeRevise,
                                                            String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(cpuInvestCheckBackHis,cpuTradeRevise);
        cpuTradeRevise.setQDATE(settleDate);
        cpuTradeRevise.setLTIME(DateUtil.getTomorrow(cpuInvestCheckBackHis.getTIM()));
        cpuTradeRevise.setQNAME(zipFileName);
        cpuTradeRevise.setDT("01");
        cpuTradeRevise.setXT(cpuInvestCheckBackHis.getFLAG());
        cpuTradeRevise.setBINF(cpuInvestCheckBackHis.getAPP()+"000000000000000000");
        cpuTradeRevise.setDMON("0000000000000");
        cpuTradeRevise.setEPID("000000000000");
        cpuTradeRevise.setETIM("00000000000000");
        cpuTradeRevise.setTAC("00000000");
        String issuea = areaService.getIssuesByCardNo(cpuInvestCheckBackHis.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        cpuTradeRevise.setUSEA(userArea);
        cpuTradeRevise.setISSUEA(issuea); //发行地
    }

    private void convertToCpuTrade(CpuInvest cpuInvest, CpuTrade cpuTrade, String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(cpuInvest,cpuTrade);
        cpuTrade.setDT("01");
        cpuTrade.setQDATE(settleDate);
        cpuTrade.setQNAME(zipFileName);
        cpuTrade.setDMON("0000000000000");
        String issuea = areaService.getIssuesByCardNo(cpuInvest.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        cpuTrade.setISSUEA(issuea); //发行地
        cpuTrade.setUSEA(userArea); //使用地
    }
    private void convertToCpuTrade(CpuInvestCheckBack cpuInvestCheckBack, CpuTrade cpuTrade, String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(cpuInvestCheckBack,cpuTrade);
        cpuTrade.setDT("01");
        cpuTrade.setQDATE(settleDate);
        cpuTrade.setQNAME(zipFileName);
        cpuTrade.setDMON("0000000000000");
        cpuTrade.setTAC("00000000");
        cpuTrade.setEPID("000000000000");
        cpuTrade.setETIM("00000000000000");
        String issuea = areaService.getIssuesByCardNo(cpuInvestCheckBack.getLCN());
        if (StringUtils.isBlank(userArea) && StringUtils.isNotBlank(issuea)){
            userArea = issuea;
        }else if (StringUtils.isBlank(issuea) && StringUtils.isNotBlank(userArea)){
            issuea = userArea;
        }
        cpuTrade.setUSEA(userArea);
        cpuTrade.setISSUEA(issuea); //发行地
    }
}
