package com.yct.settle.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/1 17:52
 */
public class SqlLdrUtil {
    private static final Logger log = LoggerFactory.getLogger(SqlLdrUtil.class);

    public static boolean insertBySqlLdr(String user,String password,String dbname,String table,String fieldName,
                                         File contrFile,File dataFile){
        log.info("开始对文件{}进行落库",dataFile.getAbsolutePath());
        boolean flag = false;
        //生成控制文件
        stlFileWriter(contrFile,dataFile,table,fieldName);
        String contrFilePath = contrFile.getAbsolutePath();
        String logPath = contrFilePath.substring(0,contrFilePath.indexOf("."))+".log";
        String command = "sqlldr " + user + "/" + password + "@" + dbname + " control=" + contrFile.getAbsolutePath() + " log="+logPath+" direct=true rows=100160 readsize=20971520 bindsize=20971520";
        // 命令。//Linux环境下注释掉不需要CMD 直接执行DOS就可以？？？
        String[] cmd = new String[] { "cmd.exe", "/C", command};
        try{
            long start = System.currentTimeMillis();
            // window执行cmd命令
            Process process = Runtime.getRuntime().exec(cmd);
            // linux执行cmd命令
            //Process process = Runtime.getRuntime().exec(command);
            //获取执行结果。0：成功
            int exitValue = process.waitFor();
            long end = System.currentTimeMillis();
            if (exitValue == 0){
                flag = true;
                log.info("导入文件{}成功，耗时{}s",dataFile.getAbsolutePath(),(end-start)/1000);
            }else {
                log.error("使用sqlldr导入文件{}失败",dataFile.getAbsolutePath());
            }
            // 关闭
            process.getOutputStream().close();
        }catch (Exception e){
            log.error("使用sqlldr导入文件{}发生异常：{}",dataFile.getAbsolutePath(),e);
        }
        return flag;
    }

    /**
     *  写控制文件.ctl
     * @param contrFile 控制文件地址路径
     * @param dataFile 数据文件
     * @param tableName 表名
     * @param fieldName 要写入表的字段:(1,2,...)
     */
    private static void stlFileWriter(File contrFile, File dataFile, String tableName, String fieldName) {
        FileWriter fw = null;
        try {
            String separator = System.getProperty("line.separator");
            String strctl = "LOAD DATA" + separator
                    + "CHARACTERSET ZHS16GBK" + separator
                    + "INFILE '" + dataFile.getAbsolutePath() +"'"+ separator
                    + "truncate INTO TABLE " + tableName + separator
                    + "FIELDS TERMINATED BY '\t'"+ separator
                    + "TRAILING NULLCOLS" +separator
                    + fieldName;

            fw = new FileWriter(contrFile);
            fw.write(strctl);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
