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
       FileUtil.zipV2("E:\\sqlldrTest\\CZ2002000120120609","E:\\sqlldrTest\\CZ2002000120120609.zip");
    }
    @Test
    public void unzipTest(){
       FileUtil.unZip(new File("E:\\testData\\input\\20121030\\XF200430120101209old.zip"));
    }

    @Test
    public void lineTest(){
        File jy = new File("E:\\testData\\input\\20120610\\CZ2065000120120609\\JY2065000120120609.txt");
        File dir = new File("E:\\testData\\output\\20120610\\CZ2065000120120609");
        FileUtil.copyJyInToOut(jy,dir);
    }

    @Test
    public void zipUnzipTest(){
        File file = FileUtil.zipUnZipFile(new File("E:\\sqlldrTest\\CZ2002000120120609"));
        System.out.println(file.getAbsolutePath());
    }


}
