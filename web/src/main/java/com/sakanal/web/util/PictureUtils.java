package com.sakanal.web.util;

import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.Picture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;

@Slf4j
public class PictureUtils {
    public static boolean downloadPicture(String downloadDir, Picture picture,InputStream inputStream ,String type){
        switch (type){
            case SourceConstant.YANDE_SOURCE: {
                return yande(downloadDir, picture);
            }
            case SourceConstant.PIXIV_SOURCE: {
                return pixiv(downloadDir, picture,inputStream);
            }
            default:{
                log.info("未知来源");
                return false;
            }
        }
    }

    private static boolean yande(String downloadDir, Picture picture) {
        InputStream inputStream;
        try {
            inputStream = new URL(picture.getSrc()).openConnection().getInputStream();
        } catch (IOException e) {
            log.info("获取图片网络地址数据失败，请检查网络情况");
            e.printStackTrace();
            return false;
        }
        String suffix = getSuffix(picture.getSrc());
        String fileName=picture.getPictureId()+"."+suffix;

        // 再获取总页数的时候就会创建文件夹

        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(downloadDir + fileName);
        } catch (FileNotFoundException e) {
            log.info("图片输出路径有误，请检查用户名是否有误");
            e.printStackTrace();
            return false;
        }
        return download(inputStream, outputStream);
    }

    private static File createDir(String downloadDir) {
        File file = new File(downloadDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.info("创建文件夹失败，请检查路径是否正确");
                return null;
            }
        }
        return file;
    }

    private static boolean pixiv(String downloadDir, Picture picture, InputStream inputStream){
        File file = createDir(downloadDir);
        if (file==null) return false;
        if (!StringUtils.hasText(picture.getSrc()))
            return false;
        String src = picture.getSrc();
        String[] split = src.split("\\.");
        String suffix = split[split.length - 1];
        String fileName = downloadDir + picture.getPictureId() + "_p" + picture.getPageCount()+ "_" + picture.getTitle()+"."+suffix;
        String targetDir = downloadDir.replace(picture.getUserName() + "-" + picture.getUserId() + "\\", "temp\\");
        String targetFileName = targetDir + picture.getPictureId() + "_p" + picture.getPageCount() + "_" + picture.getTitle() + "." + suffix;
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            log.info("无法建立图片路径，请检查图片标题是否存在无效数据");
            e.printStackTrace();
        }
        return download(inputStream, outputStream, fileName, targetDir, targetFileName);
    }

    private static boolean download(InputStream inputStream, FileOutputStream outputStream) {
        try {
            int temp;
            while ((temp=inputStream.read())!=-1){
                outputStream.write(temp);
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            log.info("下载图片失败，请检查网络是否正常，有可能不是网络问题");
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private static boolean download(InputStream inputStream, FileOutputStream outputStream, String fileName, String targetDir, String targetFileName) {
        if (download(inputStream, outputStream)) {
            // 下载成功，进行图片复制
            File originFile = new File(fileName);
            if (!originFile.exists()) {
                log.error("下载成功的文件不存在");
                // 下载的文件不存在表示该文件下载失败
                return false;
            }
            File targetFileDir = createDir(targetDir);
            if (targetFileDir == null) {
                log.info("创建temp文件夹失败，无法记录更新数据");
                // 下载的文件存在，所以认为下载成功，此时只是因为进行扩展功能时失败
                return true;
            }
            try {
                inputStream = new FileInputStream(originFile);
                outputStream = new FileOutputStream(targetFileName);
                int temp;
                while ((temp = inputStream.read()) != -1) {
                    outputStream.write(temp);
                }
                inputStream.close();
                outputStream.close();
                return true;
            } catch (FileNotFoundException e) {
                log.info("文件读取失败");
                // 下载的文件存在，所以认为下载成功，此时只是因为进行扩展功能时失败
                return true;
            } catch (IOException e) {
                log.info("文件复制失败--{}", fileName);
                // 下载的文件存在，所以认为下载成功，此时只是因为进行扩展功能时失败
                return true;
            }
        } else {
            // 下载失败
            return false;
        }
    }

    private static String getSuffix(String src){
        String[] split = src.split("\\.");
        return split[split.length-1];
    }
}
