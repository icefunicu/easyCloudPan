package com.easypan.service;

import java.io.File;

/**
 * 媒体转码服务接口.
 */
public interface MediaTranscodeService {

    /**
     * 生成缩略图（图片）.
     *
     * @param sourceFile 源文件
     * @param width 宽度
     * @param targetFile 目标文件
     * @param delSource 是否删除源文件
     * @return 是否成功
     */
    boolean createThumbnail(File sourceFile, int width, File targetFile, boolean delSource);

    /**
     * 生成视频封面.
     *
     * @param sourceFile 源文件
     * @param width 宽度
     * @param targetFile 目标文件
     */
    void createVideoCover(File sourceFile, int width, File targetFile);

    /**
     * 视频转码为 TS.
     *
     * @param sourceFilePath 源文件路径
     * @param targetTsPath 目标 TS 路径
     */
    void transcodeToTs(String sourceFilePath, String targetTsPath);

    /**
     * 切割 TS 为 M3U8.
     *
     * @param sourceTsPath 源 TS 路径
     * @param targetFolder 目标文件夹
     * @param fileId 文件 ID
     */
    void cutToM3u8(String sourceTsPath, String targetFolder, String fileId);
}
