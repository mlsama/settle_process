package com.yct.settle.manager;

import com.yct.settle.mapper.FileProcessResultMapper;
import com.yct.settle.pojo.CountData;
import com.yct.settle.pojo.CustomerService;
import com.yct.settle.pojo.DmRz;
import com.yct.settle.pojo.ProcessResult;
import com.yct.settle.service.ConsumeDataProcess;
import com.yct.settle.service.InvestDataProcess;
import com.yct.settle.service.impl.ProcessResultServiceImpl;
import com.yct.settle.thread.ThreadTaskHandle;
import com.yct.settle.utils.AmountUtil;
import com.yct.settle.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

/**
 * DESC: 处理清算数据管理
 * AUTHOR:mlsama
 * 2019/6/25 10:49
 */
@Service
public class DataProcessManager {
    //日记
    private final Logger log = LoggerFactory.getLogger(DataProcessManager.class);
    @Value("${inputDataFolder}")
    private String inputDataFolder;
    @Value(value = "${outputDataFolder}")
    private String outputDataFolder;
    @Value(value = "${resultFolder}")
    private String resultFolder;
    @Value(value = "${sqlldrFolder}")
    private String sqlldrFolder;
    //数据库信息
    @Value("${spring.datasource.c3p0.user}")
    private String dbUser;
    @Value("${spring.datasource.c3p0.password}")
    private String dbPassword;
    @Value("${spring.datasource.c3p0.odbName}")
    private String odbName;
    @Resource
    private InvestDataProcess investDataProcess;
    @Resource
    private ConsumeDataProcess consumeDataProcess;
    @Resource
    private FileProcessResultMapper fileProcessResultMapper;
    @Resource
    private ProcessResultServiceImpl processResultService;
    @Resource
    private ThreadTaskHandle threadTaskHandle;

    /**
     * 结算数据接口入口
     */
    public void settleDataProcess() {
        try {
            File inputDir = new File(inputDataFolder);
            if (inputDir.exists() && inputDir.isDirectory()){
                //创建sqlldr文件夹存放control文件
                File sqlldrDir = FileUtil.createDir(sqlldrFolder);
                //创建文件夹存放dm文件
                File resultDir = FileUtil.createDir(resultFolder);
                //获取inputDir下所有的日期文件夹
                File[] dateDirs = inputDir.listFiles();
                for (File dateDir : dateDirs){
                    processOneDayData(dateDir,sqlldrDir,resultDir);
                }
            }else {
                log.error("文件夹{}不存在或者是个文件",inputDir.getAbsolutePath());
            }
        }catch (Exception e){
            log.error("处理清算数据异常,cause by:{}",e);
        }
    }

    private File createDMFile(File dm,String prefix,String date) throws IOException {
        File file = new File(dm,prefix+date+".txt");
        if (!file.exists()){
            file.createNewFile();
        }
        return file;
    }

    /**
     * 处理一天清算后的数据，生成当天DM文件
     * @param dateDir
     */
    private void processOneDayData(File dateDir,File sqlldrDir, File resultDir) {
        if (dateDir.isDirectory()){
            threadTaskHandle.setIsError(false);
            //设定3个同步点
            CyclicBarrier barrier = new CyclicBarrier(3);
            Map<String,String> resultMap = new HashMap<>();
            File dm = null;
            try {
                //创建dm文件
                dm = FileUtil.createDmDir(resultDir.getAbsolutePath()+File.separator+"DM"+dateDir.getName());
                File dmmj = createDMFile(dm, "MJ", dateDir.getName());
                File dmmx = createDMFile(dm, "MX", dateDir.getName());
                File dmcj = createDMFile(dm, "CJ", dateDir.getName());
                File dmcx = createDMFile(dm, "CX", dateDir.getName());
                File dmfk = createDMFile(dm, "FK", dateDir.getName());
                File dmrz = createDMFile(dm, "RZ", dateDir.getName());
                Date startTime = new Date();

                //异步处理充值文件
                threadTaskHandle.handle(()->{
                    investDataProcess.processInvestFiles(inputDataFolder,outputDataFolder, dateDir.getName(), dmcj, dmcx, dmmj, dmmx,
                            dbUser, dbPassword, odbName, sqlldrDir,resultMap);
                    try {
                        //到达同步点
                        barrier.await();
                    } catch (Exception e) {
                       log.error("处理充值文件的线程到达同步点，进行线程阻塞发生异常：{}",e);
                        // 中断当前线程
                        Thread.currentThread().interrupt();
                    }
                });


                //异步处理消费文件（包括客服）
                threadTaskHandle.handle(()->{
                    consumeDataProcess.processConsumeFiles(inputDataFolder,outputDataFolder,dateDir.getName(),dmcj,dmcx,dmmj,
                            dmmx, dbUser,dbPassword,odbName,sqlldrDir,resultMap);
                    try {
                        //到达同步点
                        barrier.await();
                    } catch (Exception e) {
                        log.error("处理消费文件（包括客服）的线程到达同步点，进行线程阻塞发生异常：{}",e);
                        // 中断当前线程
                        Thread.currentThread().interrupt();
                    }
                });

                try {
                    //到达同步点
                    barrier.await();
                } catch (Exception e) {
                    log.error("主线程线程到达同步点，进行线程阻塞发生异常：{}",e);
                    // 中断当前线程
                    Thread.currentThread().interrupt();
                }

                //最后汇总
                String investResultCode = resultMap.get("investResultCode");
                String consumeResultCode = resultMap.get("consumeResultCode");
                log.info("处理结果码（0000为成功）：充值文件：{}，消费文件：{}",investResultCode,consumeResultCode);
                if ("0000".equals(investResultCode) && "0000".equals(consumeResultCode)){
                    //汇总充值数据
                    CountData investDate = fileProcessResultMapper.countInvestDate(dateDir.getName());
                    if (investDate == null){
                        investDate = new CountData();
                    }
                    //汇总消费数据
                    CountData consumeDate = fileProcessResultMapper.countConsumeDate(dateDir.getName());
                    if (consumeDate == null){
                        consumeDate = new CountData();
                    }
                    //汇总修正数据
                    CountData reviseDate = fileProcessResultMapper.countReviseDate(dateDir.getName());
                    if (reviseDate == null){
                        reviseDate = new CountData();
                    }
                    //汇总cpu充值修正数据
                    CountData cpuInvestReviseDate = fileProcessResultMapper.countCpuInvestReviseDate(dateDir.getName());
                    if (cpuInvestReviseDate == null){
                        cpuInvestReviseDate = new CountData();
                    }
                    //汇总cpu消费修正数据
                    CountData cpuConsumerReviseDate = fileProcessResultMapper.countCpuConsumerReviseDate(dateDir.getName());
                    if (cpuConsumerReviseDate == null){
                        cpuConsumerReviseDate = new CountData();
                    }
                    //汇总m1充值修正数据
                    CountData mCardInvestReviseDate = fileProcessResultMapper.countMCardInvestReviseDate(dateDir.getName());
                    if (mCardInvestReviseDate == null){
                        mCardInvestReviseDate = new CountData();
                    }
                    //汇总m1消费修正数据
                    CountData mCardConsumerReviseDate = fileProcessResultMapper.countMCardConsumerReviseDate(dateDir.getName());
                    if (mCardConsumerReviseDate == null){
                        mCardConsumerReviseDate = new CountData();
                    }

                    //汇总cpu客服数据
                    CustomerService cpuCustomerDate = fileProcessResultMapper.countCpuCustomerDate(dateDir.getName());
                    if (cpuCustomerDate == null){
                        cpuCustomerDate = new CustomerService();
                    }
                    //汇总m1客服数据
                    CustomerService mCardCustomerDate = fileProcessResultMapper.countMCardCustomerDate(dateDir.getName());
                    if (mCardCustomerDate == null){
                        mCardCustomerDate = new CustomerService();
                    }
                    long cpuCustomerNotes = cpuCustomerDate.getCpuCustomerInvestNotes()+cpuCustomerDate.getCpuCustomerConsumerNotes();
                    BigDecimal cpuCustomerAmount = AmountUtil.add(cpuCustomerDate.getCpuCustomerInvestAmount(),cpuCustomerDate.getCpuCustomerConsumerAmount());
                    long mCardCustomerNotes = mCardCustomerDate.getMCardCustomerInvestNotes()+mCardCustomerDate.getCpuCustomerConsumerNotes();
                    BigDecimal mCardCustomerAmount = AmountUtil.add(mCardCustomerDate.getMCardCustomerInvestAmount(),mCardCustomerDate.getMCardCustomerConsumerAmount());
                    long customerNotes = cpuCustomerNotes + mCardCustomerNotes;
                    BigDecimal customerAmount = AmountUtil.add(cpuCustomerAmount,mCardCustomerAmount);

                    //cpu卡汇总数据
                    CountData cpuDate = fileProcessResultMapper.countCpuDate(dateDir.getName());
                    if (cpuDate == null){
                        cpuDate = new CountData();
                    }
                    //m1卡汇总数据
                    CountData mCardDate = fileProcessResultMapper.countMCardDate(dateDir.getName());
                    if (mCardDate == null){
                        mCardDate = new CountData();
                    }
                    //充值+消费+修正
                    long totalNotes = investDate.getNotesSum() + consumeDate.getNotesSum() + reviseDate.getNotesSum();
                    BigDecimal totalAmount = AmountUtil.adds(investDate.getAmountSum(), consumeDate.getAmountSum(),reviseDate.getAmountSum());
                    //充值=充值+充值修正
                    long investNotes = investDate.getNotesSum() + cpuInvestReviseDate.getNotesSum() + mCardInvestReviseDate.getNotesSum();
                    BigDecimal investAmount = AmountUtil.adds(investDate.getAmountSum(),cpuInvestReviseDate.getAmountSum(),mCardInvestReviseDate.getAmountSum());
                    //cpu充值
                    long cpuInvestNotes = cpuDate.getInvestNotes() + cpuInvestReviseDate.getNotesSum();
                    BigDecimal cpuInvestAmount = AmountUtil.add(cpuDate.getInvestAmount(),cpuInvestReviseDate.getAmountSum());
                    //m1充值
                    long mCardInvestNotes = mCardDate.getInvestNotes() + mCardInvestReviseDate.getNotesSum();
                    BigDecimal mCardInvestAmount = AmountUtil.add(mCardDate.getInvestAmount(),mCardInvestReviseDate.getAmountSum());
                    //消费=消费+消费修正(cpu消费修正，m1消费修正)
                    long consumerNotes = consumeDate.getNotesSum() + cpuConsumerReviseDate.getNotesSum() + mCardConsumerReviseDate.getNotesSum();
                    BigDecimal consumerAmount = AmountUtil.adds(consumeDate.getConsumeAmount(),cpuInvestReviseDate.getAmountSum(),mCardInvestReviseDate.getAmountSum());
                    //cpu消费
                    long cpuConsumerNotes = cpuDate.getConsumeNotes() + cpuConsumerReviseDate.getNotesSum();
                    BigDecimal cpuConsumerAmount = AmountUtil.add(cpuDate.getConsumeAmount(),cpuConsumerReviseDate.getAmountSum());
                    //m1消费
                    long mCardConsumerNotes = mCardDate.getConsumeNotes() + mCardInvestReviseDate.getNotesSum();
                    BigDecimal mCardConsumerAmount = AmountUtil.add(mCardDate.getConsumeAmount(),mCardInvestReviseDate.getAmountSum());
                    log.info("{}日结算情况如下：总笔数：{}，总金额：{}。充值总笔数：{}，充值总金额：{}。其中cpu卡充值笔数：{}，充值金额：{}。" +
                                    "m1卡充值笔数：{}，充值金额：{}。消费总笔数：{}，消费总金额：{}。其中cpu卡消费笔数：{}，消费金额：{}。" +
                                    "m1卡消费笔数：{}，消费金额：{}。",
                            dateDir.getName(),totalNotes,totalAmount,investNotes,investAmount,cpuInvestNotes,
                            cpuInvestAmount,mCardInvestNotes,mCardInvestAmount,consumerNotes,
                            consumerAmount,cpuConsumerNotes,cpuConsumerAmount,
                            mCardConsumerNotes,mCardConsumerAmount);
                    //写入处理结果表
                    ProcessResult processResult = new ProcessResult(
                            dateDir.getName(),startTime,new Date(),"0000","处理成功",totalNotes,totalAmount,investNotes,
                            investAmount,cpuInvestNotes, cpuInvestAmount,mCardInvestNotes,
                            mCardInvestAmount,consumerNotes,consumerAmount,cpuConsumerNotes,
                            cpuConsumerAmount, mCardConsumerNotes,mCardConsumerAmount,customerNotes,
                            customerAmount,cpuCustomerNotes, cpuCustomerAmount,mCardCustomerNotes,mCardCustomerAmount,
                            reviseDate.getNotesSum(), reviseDate.getAmountSum(),cpuDate.getReviseNotes(),
                            cpuDate.getReviseAmount(),mCardDate.getReviseNotes(),mCardDate.getReviseAmount());
                    processResultService.delAndInsert(processResult);

                    //写入dmrz
                    ArrayList<DmRz> dmRzs = new ArrayList<>();
                    dmRzs.add(new DmRz(dmmj.getName(),mCardInvestNotes+mCardConsumerNotes));
                    dmRzs.add(new DmRz(dmmx.getName(),mCardDate.getReviseNotes()));
                    dmRzs.add(new DmRz(dmcj.getName(),cpuInvestNotes+cpuConsumerNotes));
                    dmRzs.add(new DmRz(dmcx.getName(),cpuDate.getReviseNotes()));
                    dmRzs.add(new DmRz(dmfk.getName(),0));
                    FileUtil.writeToFile(dmrz,dmRzs);

                    //压缩生成的DM文件
                    FileUtil.zipV2(dm.getAbsolutePath(), dm.getAbsolutePath() + ".ZIP");
                    long endTime = System.currentTimeMillis();
                    log.info("{}下的文件处理成功，耗时{}S",dateDir.getAbsolutePath(),(endTime-startTime.getTime())/1000);
                }else {
                    log.error("{}下的文件处理失败",dateDir.getAbsolutePath());
                    FileUtil.deleteFile(dm);
                    //清空文件处理结果表
                    fileProcessResultMapper.delByDate(dateDir.getName());
                }
            }catch (Exception e){
                log.error("处理{}清算后的数据发生异常:{}",dateDir.getName(),e);
                FileUtil.deleteFile(dm);
                //清空文件处理结果表
                fileProcessResultMapper.delByDate(dateDir.getName());
            }
        }else {
            log.error("{}不是个文件夹",dateDir.getAbsolutePath());
        }
    }

}
