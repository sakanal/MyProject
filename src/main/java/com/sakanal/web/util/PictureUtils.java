package com.sakanal.web.util;

import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.Picture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;

/**
 * 图片下载工具类
 * 提供从不同图片源（Pixiv、Yande）下载图片的通用方法
 * 支持下载进度统计、文件复制等功能
 *
 * @author sakanal
 */
@Slf4j
public class PictureUtils {

    /**
     * 下载图片的入口方法，根据图片来源类型选择对应的下载策略
     *
     * @param downloadDir   下载目录路径
     * @param picture       图片实体对象，包含下载所需的所有信息
     * @param inputStream   图片输入流（Pixiv使用）
     * @param type          图片来源类型（Pixiv/Yande）
     * @param userFlag      是否使用用户模式下载（true-隔离用户下载，false-普通下载）
     * @return 下载是否成功
     */
    public static boolean downloadPicture(String downloadDir, Picture picture, InputStream inputStream, String type, boolean userFlag) {
        switch (type) {
            case SourceConstant.YANDE_SOURCE: {
                return yande(downloadDir, picture);
            }
            case SourceConstant.PIXIV_SOURCE: {
                return pixiv(downloadDir, picture, inputStream, userFlag);
            }
            default: {
                log.error("未知图片来源: {}", type);
                return false;
            }
        }
    }

    /**
     * 下载图片的简化版本，默认不使用用户模式
     *
     * @param downloadDir   下载目录路径
     * @param picture       图片实体对象
     * @param inputStream   图片输入流
     * @param type          图片来源类型
     * @return 下载是否成功
     */
    public static boolean downloadPicture(String downloadDir, Picture picture, InputStream inputStream, String type) {
        return downloadPicture(downloadDir, picture, inputStream, type, false);
    }

    private static boolean yande(String downloadDir, Picture picture) {
        String suffix = getSuffix(picture.getSrc());
        String fileName = picture.getPictureId() + "." + suffix;

        // 再获取总页数的时候就会创建文件夹

        try (InputStream inputStream = new URL(picture.getSrc()).openConnection().getInputStream();
             FileOutputStream outputStream = new FileOutputStream(downloadDir + fileName)) {
            return download(inputStream, outputStream);
        } catch (IOException e) {
            log.error("获取图片网络地址数据失败，请检查网络情况,errorMessage=[{}]", e.getMessage(), e);
            return false;

        }
    }

    private static File createDir(String downloadDir) {
        if (downloadDir == null || downloadDir.trim().isEmpty()) {
            log.error("创建文件夹失败：路径为空");
            return null;
        }
        File file = new File(downloadDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.error("创建文件夹失败，请检查路径是否正确：{}", downloadDir);
                return null;
            }
        }
        return file;
    }

    private static boolean pixiv(String downloadDir, Picture picture, InputStream inputStream, boolean userFlag) {
        if (picture == null) {
            log.error("图片信息为空");
            return false;
        }
        File file = createDir(downloadDir);
        if (file == null) {
            return false;
        }
        if (!StringUtils.hasText(picture.getSrc())) {
            log.error("图片URL为空，pictureId: {}", picture.getPictureId());
            return false;
        }
        String src = picture.getSrc();
        String suffix = getSuffix(src);
        String fileName = downloadDir + picture.getPictureId() + "_p" + picture.getPageCount() + "_" + picture.getTitle() + "." + suffix;
        String targetDir;
        if (userFlag) {
            targetDir = downloadDir.replace(picture.getUserName() + "-" + picture.getUserId() + "\\", "user\\");
        } else {
            targetDir = downloadDir.replace(picture.getUserName() + "-" + picture.getUserId() + "\\", "temp\\");
        }
        String targetFileName = targetDir + picture.getPictureId() + "_p" + picture.getPageCount() + "_" + picture.getTitle() + "." + suffix;

        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            return download(inputStream, outputStream, fileName, targetDir, targetFileName);
        } catch (IOException e) {
            log.error("无法建立图片路径，请检查图片标题是否存在无效数据,errorMessage=[{}]", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 正式下载图片
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @return true-下载成功 false-下载失败
     */
    private static boolean download(InputStream inputStream, FileOutputStream outputStream) {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (IOException e) {
            log.error("下载图片失败，请检查网络是否正常，有可能不是网络问题,errorMessage=[{}]", e.getMessage(), e);
            return false;
        }
    }


    /**
     * 正式下载并复制图片
     *
     * @param inputStream    输入流
     * @param outputStream   输出流
     * @param fileName       图片名
     * @param targetDir      目标文件夹
     * @param targetFileName 目标图片名
     * @return 下载状态 true-成功 false-失败
     */
    private static boolean download(InputStream inputStream, FileOutputStream outputStream, String fileName, String targetDir, String targetFileName) {
        if (inputStream == null || outputStream == null) {
            log.error("输入流或输出流为空");
            return false;
        }

        boolean downloadSuccess = download(inputStream, outputStream);
        if (downloadSuccess) {
            // 下载成功，进行图片复制
            File originFile = new File(fileName);
            if (!originFile.exists()) {
                log.error("下载成功的文件不存在");
                // 下载的文件不存在表示该文件下载失败
                return false;
            }
            File targetFileDir = createDir(targetDir);
            if (targetFileDir == null) {
                log.error("创建temp文件夹失败，无法记录更新数据");
                // 下载的文件存在，所以认为下载成功，此时只是因为进行扩展功能时失败
                return true;
            }
            // 使用提取的copyFile方法
            copyFile(fileName, targetFileName);
            // 无论复制是否成功，只要下载成功就返回true
            return true;
        } else {
            // 下载失败
            return false;
        }
    }

    /**
     * 复制文件
     *
     * @param sourceFile 源文件路径
     * @param targetFile 目标文件路径
     */
    private static void copyFile(String sourceFile, String targetFile) {
        File originFile = new File(sourceFile);
        if (!originFile.exists()) {
            log.error("源文件不存在: {}", sourceFile);
        }

        try (FileInputStream fis = new FileInputStream(originFile);
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            log.error("文件读取失败, errorMessage=[{}]", e.getMessage(), e);
        } catch (IOException e) {
            log.error("文件复制失败--{}, errorMessage=[{}]", sourceFile, e.getMessage(), e);
        }
    }

    /**
     * 从URL中提取文件后缀
     * 如果URL格式不正确或为空，则默认返回"jpg"
     *
     * @param src 图片URL地址
     * @return 文件后缀名（不含点号）
     */
    private static String getSuffix(String src) {
        if (src == null || src.trim().isEmpty()) {
            log.error("获取文件后缀失败：URL为空");
            // 默认返回 jpg 作为后备
            return "jpg";
        }
        String[] split = src.split("\\.");
        if (split.length < 2) {
            log.error("获取文件后缀失败：URL格式不正确，无法提取后缀: {}", src);
            // 默认返回 jpg 作为后备
            return "jpg";
        }
        return split[split.length - 1];
    }
}