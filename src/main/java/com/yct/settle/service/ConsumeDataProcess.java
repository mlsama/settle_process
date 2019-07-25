package com.yct.settle.service;

import java.io.File;
import java.util.Map;

/**
 * DESC:充值数据处理
 * AUTHOR:mlsama
 * 2019/6/28 9:24
 */
public interface ConsumeDataProcess {

    boolean writerTODM(File dmmj, File dmmx, File dmcj, File dmcx, String settleDate, String zipFileName,Boolean isBusFile);

    void processConsumeFiles(String inputDataFolder, String outputDataFolder, String name, File dmcj,
                                File dmcx, File dmmj, File dmmx, String dbUser, String dbPassword, String odbName,
                                File sqlldrDir, Map<String, String> resultMap);
}
