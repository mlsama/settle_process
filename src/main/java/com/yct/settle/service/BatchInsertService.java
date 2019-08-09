package com.yct.settle.service;

import java.io.File;
import java.util.Map;

/**
 * DESC:批量导入服务
 * AUTHOR:mlsama
 * 2019/8/8 9:39
 */
public interface BatchInsertService {

    boolean batchInsertInvestData(String date, File unZipFile, String outZipFileName, String dbUser,
                                    String dbPassword, String odbName, File sqlldrDir, File targetFile, Map<String, String> resultMap);

    boolean batchInsertConsumerData(String date,File unOutZipFileDir, String inZipFileName, File unZipFile,
                                    Boolean isBusFile,File sqlldrDir, String dbUser, String dbPassword, String odbName);

    boolean batchInsertCustomer(String date,String inZipFileName, File unZipFile, File sqlldrDir,
                                    String dbUser, String dbPassword, String odbName);
}
