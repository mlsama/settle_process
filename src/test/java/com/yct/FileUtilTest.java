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
       FileUtil.zipV2("E:\\sqlldrTest","E:\\sqlldrTest.ZIP");
    }
    @Test
    public void unzipTest(){
       FileUtil.unZip(new File("E:\\sqlldrTest.zip"));
    }

    @Test
    public void lineTest(){
        File file = new File("E:\\yct\\settleData\\output\\20110309");
        System.out.println(file.getName());
    }

}
