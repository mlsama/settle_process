package com.yct;

import com.yct.settle.utils.FileUtil;
import org.junit.Test;

import java.io.File;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/11 9:31
 */
public class FileUtilTest {

    @Test
    public void createDirTest(){
        FileUtil.createDmDir("E://mlsama");
    }
    @Test
    public void zipTest(){
       FileUtil.zipV2("E:\\testData\\DM20130816","E:\\testData\\DM20130816.ZIP");
    }
    @Test
    public void unzipTest(){
       FileUtil.unZip(new File("E:\\testData\\DM20130816.ZIP"));
    }

    @Test
    public void lineTest(){
        String line = "     192\t5100000100033693\t3453C5E2\t10000160\t20020102102454\t10000160\t20020102102454\t   50.00\t   50.00\t14\t    1\t10000160\t20020102102454\t00\t0\t802FEF9F";
    }
}
