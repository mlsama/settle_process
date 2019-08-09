package com.yct.settle.service;

import java.io.File;

/** 客服数据服务接口
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/4 14:14
 */
public interface CustomerServiceDataProcess {

    boolean writerTODM(File dmmj, File dmcj, String settleDate, String inZipFileName);

}
