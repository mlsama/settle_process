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
                Boolean isBusFile = null;
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
                            boolean b;
                            for (File unZipFile : unZipFiles) {
                                if ("02".equals(zipFileType) && unZipFile.getName().startsWith("JY")) { //消费
                                    isBusFile = FileUtil.isBusFile(unZipDirName, unZipFile);
                                    //落库
                                    b = batchInsert(cOutputDateDir, unZipDirName, unZipFile, isBusFile, sqlldrDir,
                                            dbUser,dbPassword,odbName);
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
                            //从数据库取出数据写入dm
                            writerConsumeOrCustomerToDm(dmmj,dmmx,dmcj,dmcx,date,inZipFileName,isBusFile,zipFileType);
                            // 把这个消费文件的统计数据存到数据库表
                            countConsumeOrCustomerDataToDb(inZipFileName,isBusFile);
                            //删除解压的文件夹
                            FileUtil.deleteFile(unZipDir);
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

    private void countConsumeOrCustomerDataToDb(String inZipFileName,boolean isBusFile) {
        long investNotes = 0;
        BigDecimal investAmount = new BigDecimal("0");
        long consumeNotes = 0;
        BigDecimal consumeAmount = new BigDecimal("0");
        long reviseNotes = 0;
        BigDecimal reviseAmount = new BigDecimal("0");
        if (inZipFileName.startsWith("CX")){    //cpu消费文件
            if (isBusFile){ //公交
                log.info("cpu卡公交消费文件统计数据并落库");
                CountData cpuConsumeCountData = cpuConsumeMapper.countAmountAndNum();
                consumeNotes = cpuConsumeCountData.getNotesSum();
                consumeAmount = cpuConsumeCountData.getAmountSum();
                CountData cpuConsumeReviseCountData = cpuConsumeReviseMapper.countAmountAndNum();
                reviseNotes = cpuConsumeReviseCountData.getNotesSum();
                reviseAmount = cpuConsumeReviseCountData.getAmountSum();
            }else {
                log.info("cpu卡非公交消费文件统计数据并落库");
                CountData cpuConsumeCountData = cpuConsumeNoBusMapper.countAmountAndNum();
                consumeNotes = cpuConsumeCountData.getNotesSum();
                consumeAmount = cpuConsumeCountData.getAmountSum();
            }
        }else if (inZipFileName.startsWith("XF")){  //m1卡
            log.info("m1卡公交消费文件统计数据并落库");
            if (isBusFile){
                CountData mCardConsume = mCardConsumeMapper.countAmountAndNum();
                consumeNotes = mCardConsume.getNotesSum();
                consumeAmount = mCardConsume.getAmountSum();
                CountData cpuConsumeRevise = mCardConsumeReviseMapper.countAmountAndNum();
                reviseNotes = cpuConsumeRevise.getNotesSum();
                reviseAmount = cpuConsumeRevise.getAmountSum();
            }else {
                log.info("m1卡非公交消费文件统计数据并落库");
                CountData mCardConsume = mCardConsumeNoBusMapper.countAmountAndNum();
                consumeNotes = mCardConsume.getNotesSum();
                consumeAmount = mCardConsume.getAmountSum();
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
     *  把CX或者XF压缩文件中的JY文件和对应的output文件夹的相关的文件（CW,XZ）落库
     * @param cOutputDateDir input对应的output的文件夹
     * @param unZipDirName  input压缩文件解压后的文件夹名字
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
        boolean oFileExists = false;
        File cOutputUnzipDir = null;
        //解压output对应的压缩文件
        Boolean bool = false;
        File temFile = null;
        File[] zipFiles = cOutputDateDir.listFiles();
        for (File zipFile : zipFiles){
            if (zipFile.getName().startsWith(unZipDirName)){
                oFileExists = true;
                temFile = zipFile;
            }
        }
        if (oFileExists){
            //解压
            bool = FileUtil.unZip(temFile);
            if (bool){
                //output对应的文件
                cOutputUnzipDir = new File(cOutputDateDir,unZipDirName);
            }else {
                log.error("{}解压失败或者文件名{}不是以{}开头",temFile.getAbsolutePath(),temFile.getName(),unZipDirName);
                FileUtil.deleteFile(cOutputUnzipDir);
                return false;
            }
            cOutputUnzipFiles = cOutputUnzipDir.listFiles();
        }

        if (unZipDirName.startsWith("CX")){ //CPU卡
            if (isBusFile){  //公交
                tableName = "T_CPU_CONSUME";
                fieldNames = "(PID,PSN,TIM,LCN,FCN,TF,FEE,BAL,TT,ATT,CRN,XRN,DMON,BDCT,MDCT,UDCT,EPID,ETIM,LPID,LTIM,AREA,ACT,SAREA,TAC,MEM)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuConsume.ctl");
                if (oFileExists){
                    for (File cOutputUnzipFile : cOutputUnzipFiles){
                        if (cOutputUnzipFile.getName().startsWith("CW")){ //错误
                            String otable = "T_CPU_CONSUME_ERROR";
                            String ofields = "(PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, LPID, LTIM, AREA, ACT, SAREA, TAC, STATUS)";
                            File ocfile = new File(sqlldrDir,"cpuConsumeError.ctl");
                            toMap(info,otable,ofields,ocfile,cOutputUnzipFile);
                        }
                        if (cOutputUnzipFile.getName().startsWith("XZ")){ //修正
                            String otable = "T_CPU_CONSUME_REVISE";
                            String ofields = "(FNAME, PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, LPID, LTIM, AREA, ACT, SAREA, TAC, FLAG, CODE)";
                            File ocfile = new File(sqlldrDir,"cpuConsumeRevise.ctl");
                            toMap(info,otable,ofields,ocfile,cOutputUnzipFile);
                        }
                    }
                }
            }else { //非公交
                tableName = "T_CPU_CONSUME_NOBUS";
                fieldNames = "(PID, PSN, TIM, LCN, FCN, TF, FEE, BAL, TT, ATT, CRN, XRN, DMON, EPID, ETIM, LPID, LTIM, TAC)";
                //控制文件
                contlFile = new File(sqlldrDir,"cpuConsumeNoBus.ctl");
                if (oFileExists){
                    for (File cOutputUnzipFile : cOutputUnzipFiles){
                        if (cOutputUnzipFile.getName().startsWith("CW")){ //错误
                            String otable = "T_CPU_CONSUME_ERROR_NOBUS";
                            String ofields = "(PID, PSN, TAC, STATUS)";
                            File ocfile = new File(sqlldrDir,"cpuConsumeErrorNoBus.ctl");
                            toMap(info,otable,ofields,ocfile,cOutputUnzipFile);
                            break;
                        }
                    }
                }
            }
        }else if (unZipDirName.startsWith("XF")){ //M1卡
            if (isBusFile){  //公交
                tableName = "T_MCARD_CONSUME";
                fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, FEE, TT, RN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, AI, VC, TAC)";
                //控制文件
                contlFile = new File(sqlldrDir,"mCardConsume.ctl");
                if (oFileExists){
                    for (File cOutputUnzipFile : cOutputUnzipFiles){
                        if (cOutputUnzipFile.getName().startsWith("CW")){ //错误
                            String otable = "T_MCARD_CONSUME_ERROR";
                            String ofields = "(PID, PSN, STATUS)";
                            File ocfile = new File(sqlldrDir,"mCardConsumeError.ctl");
                            if (unZipDirName.startsWith("XF268")){
                                //特殊文件，CW与JY一致。转换
                                File cw = new File(cOutputUnzipDir,"cw.txt");
                                convertToCw(cOutputUnzipFile,cw);
                                toMap(info,otable,ofields,ocfile,cw);
                            }else {
                                toMap(info,otable,ofields,ocfile,cOutputUnzipFile);
                            }
                        }
                        if (cOutputUnzipFile.getName().startsWith("XZ")){ //修正
                            String otable = "T_MCARD_CONSUME_REVISE";
                            String ofields = "(FNAME, PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL, FEE, TT, RN, DMON, BDCT, MDCT, UDCT, EPID, ETIM, AI, VC, TAC, FLAG, CODE)";
                            File ocfile = new File(sqlldrDir,"mCardConsumeRevise.ctl");
                            toMap(info,otable,ofields,ocfile,cOutputUnzipFile);
                        }
                    }
                }
            }else { //非公交
                tableName = "T_MCARD_CONSUME_NOBUS";
                //XF80480001的JY多个8位余额
                if (unZipDirName.startsWith("XF80480001")){
                    fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL,FEE, TT, RN, EPID, ETIM, AI, VC, TAC, MEM)";
                }else {
                    fieldNames = "(PSN, LCN, FCN, LPID, LTIM, PID, TIM, TF, BAL,FEE constant '00000.00',TT, RN, EPID, ETIM, AI, VC, TAC, MEM)";
                }
                //控制文件
                contlFile = new File(sqlldrDir,"mCardConsumeNoBus.ctl");
                if (oFileExists){
                    for (File cOutputUnzipFile : cOutputUnzipFiles){
                        if (cOutputUnzipFile.getName().startsWith("CW")){ //错误
                            String otable = "T_MCARD_CONSUME_ERROR_NOBUS";
                            String ofields = "(PSN, PID, STATUS)";
                            File ocfile = new File(sqlldrDir,"mCardConsumeErrorNoBus.ctl");
                            toMap(info,otable,ofields,ocfile,cOutputUnzipFile);
                            break;
                        }
                    }
                }
            }
        }
        toMap(info,tableName,fieldNames,contlFile,unZipFile);
        //落库
        for (Map<String,Object> map : info){
            boolean f = SqlLdrUtil.insertBySqlLdr(dbUser,dbPassword,odbName,(String)map.get("tableName"),(String)map.get("fieldNames"),
                    (File) map.get("contlFile"),(File) map.get("dataFile"));
            if (!f){
                log.error("落库失败，文件是{}",((File) map.get("dataFile")).getAbsolutePath());
                FileUtil.deleteFile(cOutputUnzipDir);
                return false;
            }
        }
        FileUtil.deleteFile(cOutputUnzipDir);
        return true;
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
                resultLine = split[0] + "\t" + split[5] + System.getProperty("line.separator");
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
