package com.yct.sqlldr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 * DESC:sqlldr测试
 * AUTHOR:mlsama
 * 2019/7/1 15:35
 */
public class SqlLdr1 {
    //日记
    private final Logger log = LoggerFactory.getLogger(SqlLdr1.class);
    /**
     * 数据入库
     */
    public void sqlldrInsert() {
        try {
            String file_path = "";// 文件路径
            String file_name = "";// 数据文件名
            String tableName = "";// 表名
            // 要写入表的字段
            String fieldName = "(PID, PSN, TIM, LCN, FCN, TF, FEE,BAL, TT, ATT, CRN, XRN, DMON, BDCT, MDCT,UDCT, EPID, ETIM, LPID, LTIM, AREA, ACT,SAREA, TAC, MEM)";
            String ctlfileName = "";// 控制文件名
            String log_fileName = "";// 日志文件名
            File ctlfile = new File(file_path + ctlfileName);
            if (!ctlfile.exists()) {// 如果文件不存在
                try {
                    ctlfile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File logfile = new File(file_path + log_fileName);
            if (!logfile.exists()) {// 如果文件不存在
                try {
                    logfile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            stlFileWriter(file_path, file_name, tableName, fieldName, ctlfileName);
            File mkdirpath = new File(file_path);
            if (!mkdirpath.exists()) {// 如果文件夹不存在
                mkdirpath.mkdir();
            }
            // 要执行的DOS命令
            String username = "intf";
            String password = "Intf123#";
            String dbname = "crmcxdb";
            Executive(username, password, dbname, file_path, ctlfileName, mkdirpath.getAbsolutePath(), file_name,
                    log_fileName);

        } catch (Exception e) {
            log.error("文件入库失败，" + e);
        }
    }

    /**
     * * 写控制文件.ctl
     *
     * @param fileRoute   数据文件地址路径
     * @param fileName    数据文件名
     * @param tableName   表名
     * @param fieldName   要写入表的字段
     * @param ctlfileName 控制文件名
     */
    public void stlFileWriter(String fileRoute, String fileName, String tableName, String fieldName, String ctlfileName) {
        FileWriter fw = null;
        String strctl = "LOAD DATA\r\n"
                + "CHARACTERSET ZHS16GBK\r\n"
                + "INFILE '" + fileRoute + fileName + "'\r\n"
                + "APPEND INTO TABLE " + tableName + "\r\n"
                + "FIELDS TERMINATED BY '|'\r\n"
                + "TRAILING NULLCOLS\r\n"
                + fieldName;
        try {
            log.info(fileRoute + "" + ctlfileName);
            fw = new FileWriter(fileRoute + "" + ctlfileName);
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

    /**
     * 调用系统DOS命令
     * @param user
     * @param psw
     * @param fileRoute   文件路径
     * @param ctlfileName 控制文件名
     * @param logfileName 日志文件名
     */
    public void Executive(String user, String psw, String dbname, String fileRoute, String ctlfileName,
                          String mkdirpath, String file_name, String logfileName) {
        // 要执行的DOS命令
        String dos = "sqlldr " + user + "/" + psw + "@" + dbname + " control=" + fileRoute + ctlfileName
                + " log=" + fileRoute + logfileName;

        String[] cmd = {"/bin/bash", "-c", "echo $ORACLE_HOME;echo $LD_LIBRARY_PATH;$ORACLE_HOME/bin/" + dos};

        try {
            //输出环境变量
            Map<String, String> map = System.getenv();
            for (Iterator<String> itr = map.keySet().iterator(); itr.hasNext(); ) {
                String key = itr.next();
                System.out.println(key + "=" + map.get(key));
            }
            //给文件赋权为777
            Runtime.getRuntime().exec("chmod 777 -R " + fileRoute + "" + file_name);
            Runtime.getRuntime().exec("chmod 777 -R " + fileRoute + "" + ctlfileName);
            Runtime.getRuntime().exec("chmod 777 -R " + fileRoute + "" + logfileName);
            //执行sqlldr命令
            //final Process process = Runtime.getRuntime().exec("bash -c export ORACLE_HOME=/app/oracle/product/12.1.0/db;echo \"测试打印输出\"");
            final Process process = Runtime.getRuntime().exec(cmd, new String[]{"ORACLE_HOME=/app/oracle/product/12.1.0/db", "LD_LIBRARY_PATH=/usr/local/lib:/app/oracle/product/12.1.0/db/lib:$LD_LIBRARY_PATH"});

            // 处理InputStream的线程
            new Thread() {
                @Override
                public void run() {
                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line = null;
                    try {
                        while ((line = in.readLine()) != null) {
                            System.out.println("output: " + new String(line.getBytes(), "UTF-8"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
            // 处理ErrorStream的线程
            new Thread() {
                @Override
                public void run() {
                    BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String line = null;
                    try {
                        while ((line = err.readLine()) != null) {
                            System.out.println("err: " + new String(line.getBytes(), "UTF-8"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            err.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            int exitValue = process.waitFor();
        }catch (Exception e){

        }
    }
}
