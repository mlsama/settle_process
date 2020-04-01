package com.yct.settle.utils;

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Author：mlsama
 * Date: 2019-06-18
 * Desc: 文件工具类
 */
public class FileUtil {
    //日记
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);


    /**
     * 创建文件夹
     * @return
     */
    public static File createDmDir(String dirPath){
        File dir = new File(dirPath);
        if (!dir.exists()){
            dir.mkdir();
        }else {
            FileUtil.deleteFile(dir);
            File newDir = new File(dirPath);
            newDir.mkdir();
        }
        return dir;
    }

    /**
     * 创建文件夹
     * @return
     */
    public static File createDir(String dirPath){
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()){
            dir.mkdir();
        }
        return dir;
    }

    /**
     * 根据相对路径获取绝对路径（相对路径要以/开头）
     * @param fileRelative
     * @return
     */
    public static String getFileAbsolute(String fileRelative) {
        URL resource = FileUtil.class.getResource(fileRelative);
        if (resource != null){
            return resource.getPath();
        }else {
            throw new RuntimeException("文件不存在，请检查路径（要以/开头）");
        }
    }

    /**
     * 取的给定源目录下的所有文件及空的子目录
     * 递归实现
     * @param srcFile
     * @return
     */
    public static List<File> getFiles(File srcFile,List<File> fileList) {
        //文件夹
        if (srcFile.isDirectory()){
            for (File file :  srcFile.listFiles()) {
                if (file.isFile()) {
                    fileList.add(file);
                }else {
                    //若不是空目录，则递归添加其下的目录和文件
                    getFiles(file,fileList);
                }
            }
        }else {//文件
            fileList.add(srcFile);
        }
        return fileList;
    }

    /**
     * 解压zip。自动创建解压后的文件夹，与压缩文件同名，在同一个目录
     * @param zipFile zip文件
     * @return
     */
    public static Boolean unZip(File zipFile){
        long startTime = System.currentTimeMillis();
        ZipFile zip = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            String zipFilePath = zipFile.getAbsolutePath();
            String unzipFilePath = null;
            if (StringUtils.isNotEmpty(zipFilePath)) {
                unzipFilePath = zipFilePath.substring(0, zipFilePath.lastIndexOf("."));
            }
            //创建解压缩文件保存的路径
            File unzipFileDir = new File(unzipFilePath);
            if (!unzipFileDir.exists() || !unzipFileDir.isDirectory()) {
                unzipFileDir.mkdirs();
            }
            //开始解压
            ZipEntry entry = null;
            String entryFilePath = null, entryDirPath = null;
            File entryFile = null, entryDir = null;
            int count = 0;
            byte[] buffer = new byte[1024];
            //指定编码，否则压缩包里面不能有中文目录
            zip = new ZipFile(zipFile,Charset.forName("gbk"));
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>)zip.entries();
            //循环对压缩包里的每一个文件进行解压
            while(entries.hasMoreElements()) {
                entry = entries.nextElement();
                //构建压缩包中一个文件解压后保存的文件全路径
                entryFilePath = unzipFilePath + File.separator + entry.getName();
                //创建解压文件
                entryFile = new File(entryFilePath);
                //如果是文件夹
                if (entry.isDirectory()){
                    if (!entryFile.exists()){
                        entryFile.mkdir();
                    }
                }else {
                    //写入文件
                    bos = new BufferedOutputStream(new FileOutputStream(entryFile));
                    bis = new BufferedInputStream(zip.getInputStream(entry));
                    while ((count = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, count);
                    }
                    bos.flush();
                    bos.close();
                }
            }
            long endTime = System.currentTimeMillis();
            log.info("解压{}成功,耗时{}S",zipFile,(endTime-startTime)/1000);
            return true;
        }catch (Exception e){
            log.error("解压{}失败,cause by{}",zipFile,e);
            return false;
        }finally {
            if (zip != null){
                try {
                    zip.close();
                } catch (IOException e) {
                    log.error("关闭ZipFile发生异常：{}",e);
                }
            }if (bos != null){
                try {
                    bos.close();
                } catch (IOException e) {
                    log.error("关闭BufferedOutputStream发生异常：{}",e);
                }
            }if (bis != null){
                try {
                    bis.close();
                } catch (IOException e) {
                    log.error("关闭BufferedInputStream发生异常：{}",e);
                }
            }
        }
    }

    /**
     * 压缩方法(ZIP,RAR)1.0
     * （可以压缩空的子目录）
     * @param srcPath 压缩源全路径
     * @param zipFilePath 压缩后zip文件的全路径
     * @return
     */
    public static boolean zipV1(String srcPath, String zipFilePath) {
        Long startTime = System.currentTimeMillis();
        File srcFile = new File(srcPath);
        List<File> fileList = new ArrayList<>();
        //获取压缩源路径下所有要压缩的文件
        getFiles(srcFile,fileList);
        //缓冲区
        byte[] buffer  = new byte[1024];
        ZipEntry zipEntry = null;
        //每次读出来的长度
        int readLength = 0;
        ZipOutputStream zipOutputStream = null;
        try {
            //输出流指向压缩文件
            zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath));
            for (File file : fileList) {
                //获取该文件在源目录的相对路径
                String relativePath = getRelativePath(srcPath, file);
                //若是文件，则压缩这个文件
                if (file.isFile()){
                    //设置该文件在压缩文件中的路径,相对于压缩文件路径.一个zipEntry对应一个文件
                    zipEntry = new ZipEntry(relativePath);
                    zipOutputStream.putNextEntry(zipEntry);
                    //读取该文件内容
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                    while ((readLength = inputStream.read(buffer,0,1024)) != -1) {
                        //写出到压缩文件中对应的文件
                        zipOutputStream.write(buffer, 0, readLength);
                    }
                    inputStream.close();
                }else {//若是目录,则将这个目录写入zip条目
                    zipEntry = new ZipEntry(relativePath + "/");
                    zipOutputStream.putNextEntry(zipEntry);
                }
            }
            Long endTime = System.currentTimeMillis();
            log.info("压缩{}下的文件成功,耗时{}S",srcPath,(endTime-startTime)/1000);
            return true;
        } catch (Exception e) {
            log.error("压缩{}下的文件异常,cause by{}",srcPath,e);
            return false;
        }finally {
            try {
                zipOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 取相对路径
     * 依据文件名和压缩源路径得到文件在压缩源路径下的相对路径
     * @param dirPath 压缩源路径
     * @param file
     * @return 相对路径
     */
    public static String getRelativePath(String dirPath, File file) {
        File dir = new File(dirPath);
        String relativePath = file.getName();
        while (true) {
            file = file.getParentFile();
            if (file == null) {
                break;
            }
            if (file.equals(dir)) {
                break;
            } else {
                relativePath = file.getName() + "/" + relativePath;
            }
        }    // end while
        return relativePath;
    }

    /**
     * 压缩方法(ZIP,RAR)2.0
     * @param srcPath 压缩源全路径
     * @param zipFilePath 压缩后zip文件的全路径
     * @return
     */
    public static boolean zipV2(String srcPath,  String zipFilePath) {
        Long startTime = System.currentTimeMillis();
        File srcFile = new File(srcPath);
        File target = new File(zipFilePath);
        List<File> files = new ArrayList<>();
        getFiles(srcFile,files);//所有要压缩的文件
        if (files != null && files.size() > 0) {
            ZipArchiveOutputStream zaos = null;
            try {
                zaos = new ZipArchiveOutputStream(target);
                // Use Zip64 extensions for all entries where they are required
                zaos.setUseZip64(Zip64Mode.AsNeeded);
                // 将每个文件用ZipArchiveEntry封装
                // 再用ZipArchiveOutputStream写到压缩文件中
                InputStream is = null;
                for (File file : files) {
                    if (file != null) {
                        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(file, file.getName());
                        zaos.putArchiveEntry(zipArchiveEntry);
                        is = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        int len = -1;
                        while ((len = is.read(buffer)) != -1) {
                            // 把缓冲区的字节写入到ZipArchiveEntry
                            zaos.write(buffer, 0, len);
                        }
                        zaos.closeArchiveEntry();
                        is.close();
                    }
                }
            } catch (Exception e) {
                log.error("压缩文件{}发生异常，cause by:{}",srcFile,e);
               return false;
            } finally {
                try {
                    if (zaos != null) {
                        zaos.close();
                    }
                } catch (IOException e) {
                    log.error("关闭ZipArchiveOutputStream发生异常，cause by:{}",e);
                }
            }
        }
        //删除源文件
        deleteFile(srcFile);
        Long endTime = System.currentTimeMillis();
        log.info("压缩{}下的文件成功,耗时{}S",srcPath,(endTime-startTime)/1000);
        return true;
    }


    /**
     * 删除文件或者文件夹（遍历）
     * @param file
     */
    public static void deleteFile(File file) {
        if (file != null && file.exists()){
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files.length != 0){
                    for (File f : files) {
                        if (f.isDirectory()){
                            deleteFile(f);
                        }else {
                            f.delete();
                        }
                    }
                }
                // 目录此时为空，可以删除
                file.delete();
            }else {
                file.delete();
            }
            log.info("{}删除成功！",file.getAbsolutePath());
        }
    }


    public static void closeReader(Reader reader){
        if (reader != null) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("关闭读取流异常:{}", e);
                }
            }
        }
    }
    public static void closeWriter(Writer writer){
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log.error("关闭输出流异常:{}", e);
            }
        }
    }

    /**
     * 根据文件名称判断是否是公交文件
     * @return
     */
    public static Boolean isBusFile(String zipFileName,File thisFile){
        Boolean flag = false;
        if (zipFileName.startsWith("CX")){
            if (zipFileName.length() > 18){
                flag = true;
            }
        }else {
            if (zipFileName.startsWith("XF0000") || zipFileName.startsWith("XF268") ||
                    zipFileName.startsWith("XF9010") || zipFileName.startsWith("XF9013") ||
                    zipFileName.startsWith("XF9017") || zipFileName.startsWith("XF9018")){
                flag =  true;

            }else {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(thisFile)));
                    String line = reader.readLine();
                    if (line.split("\t").length > 17){
                        flag = true;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    closeReader(reader);
                }
            }
        }
        return flag;
    }

    public static synchronized void writeToFile(File file, List<?> list) {
        if (list.size() > 0){
            log.info("开始写入文件{}",file.getAbsolutePath());
            BufferedWriter bufferedWriter = null;
            try {
                //写入
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true),"UTF-8"),64);
                for (int i = 0 ; i < list.size() ; i++){
                    bufferedWriter.write(list.get(i).toString() + System.getProperty("line.separator"));
                }
                log.info("写入文件{}成功",file.getAbsolutePath());
            }catch (Exception e) {
                log.error("写入文件{}发生异常:{}",file.getAbsolutePath(),e);
            } finally {
                FileUtil.closeWriter(bufferedWriter);
            }
        }else {
            log.info("数据库没有数据，list集合为空,没有向文件{}写数据",file.getAbsolutePath());
        }
    }

    public static boolean isNewCz(File unZipFile) {
        if (unZipFile.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(unZipFile)));
                String line = reader.readLine();
                if (line.split("\t").length > 18){
                    return true;
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                closeReader(reader);
            }
            return false;
        }else {
            throw new RuntimeException(unZipFile+"不存在");
        }
    }

    public static File zipUnZipFile(File srcFile){
        //src不是压缩文件
        if (srcFile.getName().indexOf(".") == -1) {
            FileUtil.zipV2(srcFile.getAbsolutePath(),srcFile.getAbsolutePath()+".ZIP");
            srcFile = new File(srcFile.getAbsolutePath()+".ZIP");
        }
        return srcFile;
    }

    public static void copyJyInToOut(File targetFile, File unOutZipFileDir) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            File file = new File(unOutZipFileDir,targetFile.getName());
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile),"UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null){
                writer.write(line + System.getProperty("line.separator"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeReader(reader);
            closeWriter(writer);
        }

    }
}
