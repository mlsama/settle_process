package com.yct.settle.service;

import java.io.File;

/** 客服数据服务接口
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/4 14:14
 */
public interface CustomerServiceDataProcess {

    boolean batchInsert(String unZipDirName, File unZipFile, File sqlldrDir,
                                String dbUser, String dbPassword, String odbName);

    void writerTODM(File dmmj, File dmcj, String settleDate, String inZipFileName);

}
