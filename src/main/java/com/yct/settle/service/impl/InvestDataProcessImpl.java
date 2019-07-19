package com.yct.settle.service.impl;

import com.yct.settle.mapper.*;
import com.yct.settle.pojo.*;
import com.yct.settle.service.InvestDataProcess;
import com.yct.settle.thread.ThreadTaskHandle;
import com.yct.settle.utils.AmountUtil;
import com.yct.settle.utils.DateUtil;
import com.yct.settle.utils.FileUtil;
import com.yct.settle.utils.SqlLdrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class InvestDataProcessImpl implements InvestDataProcess {
    //日记
    private final Logger log = LoggerFactory.getLogger(InvestDataProcessImpl.class);
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
    private FileContentCheckMapper fileContentCheckMapper;


    /**
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
        File unZipDir = null;
        try {
            if (outputDateDir.exists()) {
                File[] outZipFiles = outputDateDir.listFiles();
                for (File outZipFile : outZipFiles) {
                    //其他线程检查
                    if (threadTaskHandle.getIsError()) {
                        log.info("有线程发生了异常，处理充值文件的线程无需再执行！");
                        return false;
                    }
                    //压缩包文件名称
                    fileName = outZipFile.getName();
                    if (fileName.startsWith("CC") || fileName.startsWith("CZ")) {
                        //对应的input文件夹
                        File inputDateDir = new File(inputDataFolder + File.separator + date);
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
                        //解压
                        Boolean flag = FileUtil.unZip(outZipFile);
                        if (flag) {
                            boolean isNewCz = false;
                            //进入解压后的文件夹
                            unZipDir = new File(outputDateDir, outZipFile.getName().substring(0, outZipFile.getName().indexOf(".")));
                            File[] unZipFiles = unZipDir.listFiles();

                            for (File unZipFile : unZipFiles){
                                if (fileName.startsWith("CZ") && unZipFile.getName().startsWith("JY")){
                                    //根据JY行长度判断格式
                                    isNewCz = FileUtil.isNewCz(unZipFile);
                                    break;
                                }
                            }

                            for (File unZipFile : unZipFiles) {
                                if (!(unZipFile.getName().startsWith("MD") || unZipFile.getName().startsWith("RZ") || unZipFile.getName().startsWith("QS"))) {
                                    //落库
                                    boolean insertFlag = batchInsert(inputDateDir,unZipFile, fileName, dbUser, dbPassword, odbName, sqlldrDir,isNewCz,resultMap);
                                    if (!insertFlag) { //JY,CZ,XZ,LC任一个文件落库失败，直接返回
                                        //修改
                                        processResultService.update(new FileProcessResult(fileName, unZipFile.getAbsolutePath(), new Date(), "6555", resultMap.get("msg")));
                                        //删除解压的文件夹
                                        FileUtil.deleteFile(unZipDir);
                                        return false;
                                    }
                                }
                            }
                            //从数据库取出数据写入dm
                            writerToDm(fileName, date, dmmj, dmmx, dmcj, dmcx);
                            //把这个充值文件的统计数据存到数据库表
                            countInvestDataToDb(fileName);
                            //删除解压的文件夹
                            FileUtil.deleteFile(unZipDir);
                            resultMap.put("investResultCode", "0000");
                        } else {
                            threadTaskHandle.setIsError(true);
                            log.error("处理充值文件{}发生异常,修改标志，通知其他线程", outZipFile);
                            //删除解压的文件夹
                            FileUtil.deleteFile(unZipDir);
                            //修改
                            processResultService.update(new FileProcessResult(fileName, outZipFile.getAbsolutePath(), new Date(), "6555", "解压失败"));
                            return false;
                        }
                    }
                }
            } else {
                log.error("文件夹{}不存在", outputDateDir.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            threadTaskHandle.setIsError(true);
            log.error("处理充值文件{}发生异常：{},修改标志，通知其他线程", fileName, e);
            //删除解压的文件夹
            FileUtil.deleteFile(unZipDir);
            //修改
            processResultService.update(new FileProcessResult(fileName, null, new Date(), "6555", "处理充值文件发生异常"));
            return false;
        }
        return true;
    }


    /**
     * 统计并落库
     * @param zipFileName
     */
    private void countInvestDataToDb(String zipFileName) {
        long investNotes = 0L;
        BigDecimal investAmount = new BigDecimal("0");
        long reviseNotes = 0L;
        BigDecimal reviseAmount  = new BigDecimal("0") ;
        if (zipFileName.startsWith("CC")){
            log.info("统计cpu卡充值,修正的笔数和金额");
            CountData investCountData = addContDate(cpuInvestMapper.countData(), cpuInvestCheckBackMapper.countData());
            investNotes = investCountData.getNotesSum();
            investAmount = investCountData.getAmountSum();

            CountData reviseCountData = addContDate(cpuInvestCheckBackHisMapper.countData(), cpuInvestReviseHisMapper.countData());
            reviseNotes = reviseCountData.getNotesSum();
            reviseAmount = reviseCountData.getAmountSum();
        }else {
            log.info("统计m1卡充值，修正的笔数和金额");
            CountData investCountData = addContDate(mCardInvestMapper.countData(), mCardInvestCheckBackMapper.countData());
            investNotes = investCountData.getNotesSum();
            investAmount = investCountData.getAmountSum();

            CountData reviseCountData = addContDate(mCardInvestCheckBackHisMapper.countData(), mCardInvestReviseHisMapper.countData());
            reviseNotes = reviseCountData.getNotesSum();
            reviseAmount = reviseCountData.getAmountSum();
        }
        processResultService.update(
                new FileProcessResult(zipFileName,new Date(),"0000","处理成功", investNotes,investAmount,
                                    0L,new BigDecimal("0"),reviseNotes,reviseAmount));
    }

    private CountData addContDate(CountData v1,CountData v2){
        v1.setNotesSum(v1.getNotesSum()+v2.getNotesSum());
        v1.setAmountSum(AmountUtil.add(v1.getAmountSum(),v2.getAmountSum()));
        return v1;
    }

    private void writerToDm(String zipFileName, String settleDate, File dmmj, File dmmx, File dmcj, File dmcx) {
        log.info("从数据库取充值数据写到对应的dm文件");
        if (zipFileName.startsWith("CC")){ //cpu卡
            ArrayList<CpuTrade> cpuTradeList = new ArrayList<>();
            //获取总笔数
            long allNotes = cpuInvestMapper.findAllNotes();
            if (allNotes > 0L) {
                long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                while (allNotes > 0L) {
                    if (allNotes <= pageSize) {
                        pageSize = allNotes;
                    }
                    endNum = startNum + pageSize;
                    log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                    List<CpuInvest> cpuInvestList = cpuInvestMapper.findByWhere(startNum,endNum);
                    if (cpuInvestList.size() > 0){
                        for (CpuInvest cpuInvest : cpuInvestList){
                            CpuTrade cpuTrade = new CpuTrade();
                            convertToCpuTrade(cpuInvest,cpuTrade,settleDate,zipFileName);
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
            List<CpuInvestCheckBack> cpuInvestCheckBackList = cpuInvestCheckBackMapper.findByWhere();
            if (cpuInvestCheckBackList.size() > 0){
                for (CpuInvestCheckBack cpuInvestCheckBack : cpuInvestCheckBackList){
                    CpuTrade cpuTrade = new CpuTrade();
                    convertToCpuTrade(cpuInvestCheckBack,cpuTrade,settleDate,zipFileName);
                    cpuTradeList.add(cpuTrade);
                }
            }
            FileUtil.writeToFile(dmcj, cpuTradeList);
            //修正
            ArrayList<CpuTradeRevise> cpuTradeReviseList = new ArrayList<>();
            List<CpuInvestReviseHis> cpuInvestReviseHisList = cpuInvestReviseHisMapper.findList();
            if (cpuInvestReviseHisList.size() > 0){
                for (CpuInvestReviseHis cpuInvestReviseHis : cpuInvestReviseHisList){
                    CpuTradeRevise cpuTradeRevise = new CpuTradeRevise();
                    convertToCpuTradeRevise(cpuInvestReviseHis,cpuTradeRevise,settleDate,zipFileName);
                    cpuTradeReviseList.add(cpuTradeRevise);
                }
            }
            List<CpuInvestCheckBackHis> investCheckBackHisList = cpuInvestCheckBackHisMapper.findList();
            if (investCheckBackHisList.size() > 0){
                for (CpuInvestCheckBackHis cpuInvestCheckBackHis : investCheckBackHisList){
                    CpuTradeRevise cpuTradeRevise = new CpuTradeRevise();
                    convertToCpuTradeRevise(cpuInvestCheckBackHis,cpuTradeRevise,settleDate,zipFileName);
                    cpuTradeReviseList.add(cpuTradeRevise);
                }
            }
            FileUtil.writeToFile(dmcx, cpuTradeReviseList);

        }else { //m1卡
            ArrayList<MCardTrade> mCardTradeList = new ArrayList<>();
            //获取总笔数
            long allNotes = mCardInvestMapper.findAllNotes();
            if (allNotes > 0L) {
                long pageSize = 100000L, startNum = 0L, endNum, count = 1L;
                while (allNotes > 0L) {
                    if (allNotes <= pageSize) {
                        pageSize = allNotes;
                    }
                    endNum = startNum + pageSize;
                    log.info("第{}次循环，allNotes={},pageSize={},startNum={},endNum={}", count, allNotes, pageSize, startNum, endNum);
                    List<MCardInvest> mCardInvestList = mCardInvestMapper.findByWhere(startNum,endNum);
                    if (mCardInvestList.size() > 0){
                        for (MCardInvest mCardInvest : mCardInvestList){
                            MCardTrade mCardTrade = new MCardTrade();
                            convertToMCardTrade(mCardInvest,mCardTrade,settleDate,zipFileName);
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

            List<MCardInvestCheckBack> mCardInvestCheckBackList = mCardInvestCheckBackMapper.findByWhere();
            if (mCardInvestCheckBackList.size() > 0){
                for (MCardInvestCheckBack mCardInvestCheckBack : mCardInvestCheckBackList){
                    MCardTrade mCardTrade = new MCardTrade();
                    convertToMCardTrade(mCardInvestCheckBack,mCardTrade,settleDate,zipFileName);
                    mCardTradeList.add(mCardTrade);
                }
            }
            FileUtil.writeToFile(dmmj, mCardTradeList);

            //修正
            ArrayList<MCardTradeRevise> mCardTradeReviseList = new ArrayList<>();
            List<MCardInvestReviseHis> mCardInvestReviseHisList = mCardInvestReviseHisMapper.findList();
            if (mCardInvestReviseHisList.size() > 0){
                for (MCardInvestReviseHis mCardInvestReviseHis : mCardInvestReviseHisList){
                    MCardTradeRevise mCardTradeRevise = new MCardTradeRevise();
                    convertToMCardTradeRevise(mCardInvestReviseHis,mCardTradeRevise,settleDate,zipFileName);
                    mCardTradeReviseList.add(mCardTradeRevise);
                }
            }
            List<MCardInvestCheckBackHis> mCardInvestCheckBackHisList = mCardInvestCheckBackHisMapper.findList();
            if (mCardInvestCheckBackHisList.size() > 0){
                for (MCardInvestCheckBackHis mCardInvestCheckBackHis : mCardInvestCheckBackHisList){
                    MCardTradeRevise mCardTradeRevise = new MCardTradeRevise();
                    convertToMCardTradeRevise(mCardInvestCheckBackHis,mCardTradeRevise,settleDate,zipFileName);
                    mCardTradeReviseList.add(mCardTradeRevise);
                }
            }
            FileUtil.writeToFile(dmmx,mCardTradeReviseList);
        }
    }

    private void convertToMCardTradeRevise(MCardInvestCheckBackHis mCardInvestCheckBackHis, MCardTradeRevise mCardTradeRevise, String settleDate, String zipFileName) {
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
    }
    private void convertToMCardTradeRevise(MCardInvestReviseHis mCardInvestReviseHis, MCardTradeRevise mCardTradeRevise, String settleDate, String zipFileName) {
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
    }

    private void convertToMCardTrade(MCardInvestCheckBack mCardInvestCheckBack, MCardTrade mCardTrade, String settleDate, String zipFileName) {
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
    }
    private void convertToMCardTrade(MCardInvest mCardInvest, MCardTrade mCardTrade, String settleDate, String zipFileName) {
        BeanUtils.copyProperties(mCardInvest,mCardTrade);
        mCardTrade.setQDATE(settleDate);
        mCardTrade.setQNAME(zipFileName);
        mCardTrade.setDT("01");
        mCardTrade.setDSN(mCardInvest.getPSN());
        mCardTrade.setICN(mCardInvest.getLCN());
        mCardTrade.setFEE(new BigDecimal("0"));
        mCardTrade.setBINF(mCardInvest.getAPP()+"000000000000000000");
    }

    private void convertToCpuTradeRevise(CpuInvestReviseHis cpuInvestReviseHis, CpuTradeRevise cpuTradeRevise, String settleDate, String zipFileName) {
        BeanUtils.copyProperties(cpuInvestReviseHis,cpuTradeRevise);
        cpuTradeRevise.setQDATE(settleDate);
        cpuTradeRevise.setLTIME(DateUtil.getTomorrow(cpuInvestReviseHis.getTIM()));
        cpuTradeRevise.setQNAME(zipFileName);
        cpuTradeRevise.setDT("01");
        cpuTradeRevise.setXT(cpuInvestReviseHis.getFLAG());
        cpuTradeRevise.setBINF(cpuInvestReviseHis.getAPP()+"000000000000000000");
        cpuTradeRevise.setDMON("0000000000000");
    }
    private void convertToCpuTradeRevise(CpuInvestCheckBackHis cpuInvestCheckBackHis, CpuTradeRevise cpuTradeRevise, String settleDate, String zipFileName) {
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
    }

    private void convertToCpuTrade(CpuInvest cpuInvest, CpuTrade cpuTrade, String settleDate, String zipFileName) {
        BeanUtils.copyProperties(cpuInvest,cpuTrade);
        cpuTrade.setDT("01");
        cpuTrade.setQDATE(settleDate);
        cpuTrade.setQNAME(zipFileName);
        cpuTrade.setDMON("0000000000000");
    }
    private void convertToCpuTrade(CpuInvestCheckBack cpuInvestCheckBack, CpuTrade cpuTrade, String settleDate, String zipFileName) {
        BeanUtils.copyProperties(cpuInvestCheckBack,cpuTrade);
        cpuTrade.setDT("01");
        cpuTrade.setQDATE(settleDate);
        cpuTrade.setQNAME(zipFileName);
        cpuTrade.setDMON("0000000000000");
        cpuTrade.setTAC("00000000");
        cpuTrade.setEPID("000000000000");
        cpuTrade.setETIM("00000000000000");
    }

    /**
     * sqlldr批量导入
     *
     * @param inputDateDir
     * @param unZipFile
     * @param dbUser
     * @param dbPassword
     * @param sqlldrDir
     * @param resultMap
     * @return
     */
    public boolean batchInsert(File inputDateDir, File unZipFile, String zipFileName, String dbUser,
                               String dbPassword, String odbName, File sqlldrDir, boolean isNewCz, Map<String, String> resultMap) {
        //input对应文件夹下的压缩文件
        File inputZipFile = new File(inputDateDir,zipFileName);
        File targetFile = null;
        File inputUnZipDir = null;
        if (inputZipFile.exists()){
            //解压
            FileUtil.unZip(inputZipFile);
            //进入解压目录
            inputUnZipDir = new File(inputDateDir,zipFileName.substring(0,zipFileName.indexOf(".")));
            targetFile = new File(inputUnZipDir,unZipFile.getName());
        }else {
            resultMap.put("msg",inputZipFile.getAbsolutePath()+"不存在");
            return false;
        }
        //表名
        String tableName = null;
        //表字段
        String fieldNames = null;
        //控制文件
        File contlFile = null;
        String fileName = unZipFile.getName();
        if (zipFileName.startsWith("CC")) {  //cpu卡充值
            if (fileName.startsWith("JY")){
                tableName = "T_CPU_INVEST";
                fieldNames = "(PID, PSN, TIM, " +
                        "      LCN, FCN, TF, FEE, " +
                        "      BAL, TT, ATT, CRN, " +
                        "      XRN, DMON, BDCT, MDCT, " +
                        "      UDCT, EPID, ETIM, LPID, " +
                        "      LTIM, AREA, ACT, SAREA, " +
                        "      TAC, APP, FLAG, ERRNO)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuInvest.ctl");
                boolean f1 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,targetFile);
                if (!f1){
                    resultMap.put("msg",targetFile.getAbsolutePath()+"落库失败");
                    FileUtil.deleteFile(inputUnZipDir);
                    return f1;
                }
                //汇总
                CountData countData = cpuInvestMapper.countAllData();
                long inputInvestNotes = 0L;
                BigDecimal inputInvestAmount = new BigDecimal("0");
                if (countData != null){
                    inputInvestNotes = countData.getNotesSum();
                    inputInvestAmount = countData.getAmountSum();
                }
                boolean f2 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,unZipFile);
                if (!f2){
                    resultMap.put("msg",unZipFile.getAbsolutePath()+"落库失败");
                    FileUtil.deleteFile(inputUnZipDir);
                    return f2;
                }
                //汇总
                CountData oCountData = cpuInvestMapper.countAllData();
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
                    fileContentCheckMapper.insert(new FileContentCheck(inputDateDir.getName(),unZipFile.getName(),"01","01",
                                                    "6555","input文件的笔数或者金额不等于output对应的文件",inputInvestNotes,
                                                    inputInvestAmount,outputInvestNotes,outputInvestAmount,0L,0L,
                                                    new BigDecimal("0"),new BigDecimal("0")));
                    resultMap.put("msg","input文件的笔数或者金额不等于output对应的文件");
                    FileUtil.deleteFile(inputUnZipDir);
                    return false;
                }
                FileUtil.deleteFile(inputUnZipDir);
                return true;

            }else if (fileName.startsWith("CZ")){
                tableName = "T_CPU_INVEST_CHECKBACK";
                fieldNames = "(PID, PSN, TIM, " +
                        "      LCN, FCN, TF, FEE, " +
                        "      BAL, TT, ATT, CRN, " +
                        "      XRN, LPID, LTIM, APP, " +
                        "      FLAG, ERRNO)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuInvestCheckBack.ctl");
            }else if (fileName.startsWith("XZ")){
                tableName = "T_CPU_INVEST_REVISE_HIS";
                fieldNames = "(PID, PSN, TIM, " +
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
                fieldNames = "(PID, PSN, TIM, " +
                        "      LCN, FCN, TF, FEE, " +
                        "      BAL, TT, ATT, CRN, " +
                        "      XRN, LPID, LTIM, APP, " +
                        "      FLAG, ERRNO)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuInvestCheckBackHis.ctl");
            }
        }else { //m1卡充值
            if (isNewCz){
                if (fileName.startsWith("JY")){
                    tableName = "T_MCARD_INVEST";
                    fieldNames = "(PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      EPID, ETIM, AI, VC, " +
                            "      TAC, APP, FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvest.ctl");

                    boolean f1 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,targetFile);
                    if (!f1){
                        resultMap.put("msg",targetFile.getAbsolutePath()+"落库失败");
                        FileUtil.deleteFile(inputUnZipDir);
                        return f1;
                    }
                    //汇总
                    CountData countData = mCardInvestMapper.countAllData();
                    long inputInvestNotes = 0L;
                    BigDecimal inputInvestAmount = new BigDecimal("0");
                    if (countData != null){
                        inputInvestNotes = countData.getNotesSum();
                        inputInvestAmount = countData.getAmountSum();
                    }
                    boolean f2 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,unZipFile);
                    if (!f2){
                        resultMap.put("msg",unZipFile.getAbsolutePath()+"落库失败");
                        FileUtil.deleteFile(inputUnZipDir);
                        return f2;
                    }
                    //汇总
                    CountData oCountData = mCardInvestMapper.countAllData();
                    long outputInvestNotes = 0L;
                    BigDecimal outputInvestAmount = new BigDecimal("0");
                    if (countData != null){
                        outputInvestNotes = oCountData.getNotesSum();
                        outputInvestAmount = oCountData.getAmountSum();
                    }
                    if (inputInvestNotes != outputInvestNotes || inputInvestAmount.compareTo(outputInvestAmount) != 0){
                        log.error("input文件[{}]的笔数或者金额不等于output对应的文件。input笔数：{}，金额:{}。output笔数：{}，金额:{}",
                                unZipFile.getAbsolutePath(),inputInvestNotes,inputInvestAmount,outputInvestNotes,outputInvestAmount);
                        //插入文件检查表
                        fileContentCheckMapper.insert(new FileContentCheck(inputDateDir.getName(),unZipFile.getName(),"01","02",
                                "6555","input文件的笔数或者金额不等于output对应的文件",inputInvestNotes,
                                inputInvestAmount,outputInvestNotes,outputInvestAmount,0L,0L,
                                new BigDecimal("0"),new BigDecimal("0")));
                        resultMap.put("msg","input文件的笔数或者金额不等于output对应的文件");
                        FileUtil.deleteFile(inputUnZipDir);
                        return false;
                    }
                    FileUtil.deleteFile(inputUnZipDir);
                    return true;

                }else if (fileName.startsWith("CZ")){
                    tableName = "T_MCARD_INVEST_CHECKBACK";
                    fieldNames = "(PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP, FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvestCheckBack.ctl");
                }else if (fileName.startsWith("XZ")){
                    tableName = "T_MCARD_INVEST_REVISE_HIS";
                    fieldNames = "(PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      EPID, ETIM, AI, VC, " +
                            "      TAC,APP,FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvestReviseHis.ctl");
                }else if (fileName.startsWith("LC")){
                    tableName = "T_MCARD_INVEST_CHECKBACK_HIS";
                    fieldNames = "(PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP, FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvestCheckBackHis.ctl");
                }
            }else {
                if (fileName.startsWith("JY")){
                    tableName = "T_MCARD_INVEST";
                    fieldNames = "(PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      EPID, ETIM, AI, VC, " +
                            "      TAC, APP constant 'FF', FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvest.ctl");

                    boolean f1 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,targetFile);
                    if (!f1){
                        resultMap.put("msg",targetFile.getAbsolutePath()+"落库失败");
                        FileUtil.deleteFile(inputUnZipDir);
                        return f1;
                    }
                    //汇总
                    CountData countData = mCardInvestMapper.countAllData();
                    long inputInvestNotes = 0L;
                    BigDecimal inputInvestAmount = new BigDecimal("0");
                    if (countData != null){
                        inputInvestNotes = countData.getNotesSum();
                        inputInvestAmount = countData.getAmountSum();
                    }
                    boolean f2 = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,unZipFile);
                    if (!f2){
                        resultMap.put("msg",unZipFile.getAbsolutePath()+"落库失败");
                        FileUtil.deleteFile(inputUnZipDir);
                        return f2;
                    }
                    //汇总
                    CountData oCountData = mCardInvestMapper.countAllData();
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
                        fileContentCheckMapper.insert(new FileContentCheck(inputDateDir.getName(),unZipFile.getName(),"01","02",
                                "6555","input文件的笔数或者金额不等于output对应的文件",inputInvestNotes,
                                inputInvestAmount,outputInvestNotes,outputInvestAmount,0L,0L,
                                new BigDecimal("0"),new BigDecimal("0")));
                        resultMap.put("msg","input文件的笔数或者金额不等于output对应的文件");
                        FileUtil.deleteFile(inputUnZipDir);
                        return false;
                    }
                    FileUtil.deleteFile(inputUnZipDir);
                    return true;

                }else if (fileName.startsWith("CZ")){
                    tableName = "T_MCARD_INVEST_CHECKBACK";
                    fieldNames = "(PSN constant '00000000', LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP constant 'FF', FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvestCheckBack.ctl");
                }else if (fileName.startsWith("XZ")){
                    tableName = "T_MCARD_INVEST_REVISE_HIS";
                    fieldNames = "(PSN, LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      EPID, ETIM, AI, VC, " +
                            "      TAC, APP constant 'FF', FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvestReviseHis.ctl");
                }else if (fileName.startsWith("LC")){
                    tableName = "T_MCARD_INVEST_CHECKBACK_HIS";
                    fieldNames = "(PSN constant '00000000', LCN, FCN, " +
                            "      LPID, LTIM, PID, TIM, " +
                            "      TF, BAL, TT, RN, " +
                            "      APP constant 'FF', FLAG, ERRNO)";
                    //控制文件
                    contlFile = new File(sqlldrDir,"mCardInvestCheckBackHis.ctl");
                }
            }
        }
        boolean flag = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,tableName,fieldNames,contlFile,unZipFile);
        if (!flag){
            resultMap.put("msg",unZipFile.getAbsolutePath()+"落库失败");
        }
        return flag;
    }
}
