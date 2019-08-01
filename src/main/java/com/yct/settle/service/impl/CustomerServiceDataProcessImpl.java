package com.yct.settle.service.impl;

import com.yct.settle.mapper.CpuCustomerServiceMapper;
import com.yct.settle.mapper.MCardCustomerServiceMapper;
import com.yct.settle.pojo.*;
import com.yct.settle.service.AreaService;
import com.yct.settle.service.CustomerServiceDataProcess;
import com.yct.settle.service.ProcessResultService;
import com.yct.settle.utils.FileUtil;
import com.yct.settle.utils.SqlLdrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/4 14:17
 */
@Service
public class CustomerServiceDataProcessImpl implements CustomerServiceDataProcess {
    private final Logger log = LoggerFactory.getLogger(CustomerServiceDataProcessImpl.class);
    @Resource
    private CpuCustomerServiceMapper cpuCustomerServiceMapper;
    @Resource
    private MCardCustomerServiceMapper mCardCustomerServiceMapper;
    @Resource
    private ProcessResultService processResultService;
    @Resource
    private AreaService areaService;

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
    public boolean batchInsert(String date,String inZipFileName, File unZipFile, File sqlldrDir,
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


    @Override
    public boolean writerTODM(File dmmj, File dmcj, String settleDate, String zipFileName) {
        try {
            //根据服务商代码确定卡使用地：USEA
            String userArea = areaService.getUseAreaByMerchant(zipFileName);
            if (zipFileName.startsWith("CK")) { //cpu卡
                ArrayList<CpuTrade> cpuTradeList = new ArrayList<>();
                List<CpuCustomerService> customerServiceList = cpuCustomerServiceMapper.findList(settleDate,zipFileName);
                for (CpuCustomerService cpuCustomerService : customerServiceList){
                    CpuTrade cpuTrade = new CpuTrade();
                    convertToCpuTrade(cpuCustomerService,cpuTrade,settleDate,zipFileName,userArea);
                    cpuTradeList.add(cpuTrade);
                }
                FileUtil.writeToFile(dmcj,cpuTradeList);
            }else { //m1卡
                ArrayList<MCardTrade> mCardTradeList = new ArrayList<>();
                List<MCardCustomerService> list = mCardCustomerServiceMapper.findList(settleDate,zipFileName);
                for (MCardCustomerService mCardCustomerService : list){
                    MCardTrade mCardTrade = new MCardTrade();
                    convertToMCardTrade(mCardCustomerService,mCardTrade,settleDate,zipFileName,userArea);
                    mCardTradeList.add(mCardTrade);
                }
                FileUtil.writeToFile(dmmj,mCardTradeList);
            }
            return true;
        }catch (Exception e){
            log.error("把客服文件{}写到dm文件发生异常:{}。",zipFileName,e);
            //修改
            processResultService.update(
                    new FileProcessResult(zipFileName, null, new Date(),
                            "6555", "把客服文件写到dm文件发生异常"));
            return false;
        }
    }

    private void convertToMCardTrade(MCardCustomerService mCardCustomerService, MCardTrade mCardTrade,
                                     String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(mCardCustomerService,mCardTrade);
        mCardTrade.setDSN(mCardCustomerService.getPSN());
        mCardTrade.setICN(mCardCustomerService.getLCN());
        mCardTrade.setQDATE(settleDate);
        mCardTrade.setDT("03");//客服
        mCardTrade.setBINF("00000000000000000000");//备用信息
        mCardTrade.setQNAME(zipFileName);
        mCardTrade.setUSEA(userArea);//使用地
        String issuea = areaService.getIssuesByCardNo(mCardCustomerService.getLCN());
        mCardTrade.setISSUEA(issuea); //发行地
    }

    private void convertToCpuTrade(CpuCustomerService cpuCustomerService, CpuTrade cpuTrade,
                                   String settleDate, String zipFileName,String userArea) {
        BeanUtils.copyProperties(cpuCustomerService,cpuTrade);
        cpuTrade.setQDATE(settleDate);
        cpuTrade.setDT("03");//客服
        cpuTrade.setDMON("0000000000000");//扩展信息
        cpuTrade.setQNAME(zipFileName);
        cpuTrade.setUSEA(userArea);//使用地
        String issuea = areaService.getIssuesByCardNo(cpuCustomerService.getLCN());
        cpuTrade.setISSUEA(issuea); //发行地
    }
}
