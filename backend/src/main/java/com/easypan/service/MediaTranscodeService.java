package com.easypan.service;

import java.io.File;

public interface MediaTranscodeService {
    
    /**
     * 生成缩略图 (图片)
     */
    boolean createThumbnail(File sourceFile, int width, File targetFile, boolean delSource);

    /**
     * 生成视频封面
     */
    void createVideoCover(File sourceFile, int width, File targetFile);

    /**
     * 视频转码为 TS
     */
    void transcodeToTs(String sourceFilePath, String targetTsPath);

    /**
     * 切割 TS 为 M3U8
     */
    void cutToM3u8(String sourceTsPath, String targetFolder, String fileId);
}
