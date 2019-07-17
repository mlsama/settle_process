package com.yct.sqlldr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/1 15:54
 */
public class SqlLdr2 {
    //日记
    private static final Logger log = LoggerFactory.getLogger(SqlLdr2.class);

    public static void Execute() {
        long start = System.currentTimeMillis();
        InputStream ins = null;
        /**
         * direct -- 使用直通路径方式导入 (默认FALSE)
         *       如果表中有索引的话，是不能指定direct=TRUE的，除非使用skip_index_maintenance=TRUE，这个就是在导入的时候忽略索引，
         *       所以在数据导入完毕以后，查看索引的状态应该都是无效的，需要重建之,如下SQL语句
                    select  * from dba_indexes where table_name='?'
                    alter  idnex index_name rebuild

            rows --每次提交的记录数，默认: 64。不设置这个无法提交大量的数据，程序一直在跑，要关掉程序才会提交
            readsize --每次读取的数据
            bindsize --每次提交记录的缓冲区的大小，字节为单位，默认256000
         */
        String command = "sqlldr SCOTT/ml@orcl control=E:\\insertTest.ctl log=E:\\export.log direct=true rows=100160 readsize=20971520 bindsize=20971520";
        // 命令。//Linux环境下注释掉不需要CMD 直接执行DOS就可以？？？
        String[] cmd = new String[] { "cmd.exe", "/C", command};
        try{
            // window执行cmd命令
            Process process = Runtime.getRuntime().exec(cmd);
            // linux执行cmd命令
            //Process process = Runtime.getRuntime().exec(command);
            // 获取执行cmd命令后的信息
            process.getInputStream();
            /*BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String msg = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                System.out.println(msg); // 输出
            }*/
            //获取执行结果。0：成功
            int exitValue = process.waitFor();
            System.out.println("Returned value was：" + exitValue);
            if(exitValue == 0) {
                System.out.println("The records were loaded successfully");
            }else {
                System.out.println("The records were not loaded successfully");
            }
            // 关闭
            process.getOutputStream().close();
        }catch (Exception e){
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        log.info("耗时{}s",(end/start)/1000);
    }

}
