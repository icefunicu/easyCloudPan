package com.easypan.service.impl;

import com.easypan.exception.BusinessException;
import com.easypan.service.MediaTranscodeService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service("mediaTranscodeService")
public class MediaTranscodeServiceImpl implements MediaTranscodeService {

    private static final Logger logger = LoggerFactory.getLogger(MediaTranscodeServiceImpl.class);

    @Override
    public boolean createThumbnail(File sourceFile, int width, File targetFile, boolean delSource) {
        try {
            BufferedImage src = ImageIO.read(sourceFile);
            if (src == null) {
                // Not an image or unsupported format
                return false;
            }
            int sourceW = src.getWidth();
            // 小于指定宽度不压缩
            if (sourceW <= width) {
                return false;
            }
            
            // ffmpeg -i %s -vf scale=%d:-1 %s -y
            List<String> cmd = new ArrayList<>();
            cmd.add("ffmpeg");
            cmd.add("-i");
            cmd.add(sourceFile.getAbsolutePath());
            cmd.add("-vf");
            cmd.add("scale=" + width + ":-1");
            cmd.add(targetFile.getAbsolutePath());
            cmd.add("-y");

            executeCommand(cmd);

            if (delSource) {
                FileUtils.forceDelete(sourceFile);
            }
            return true;
        } catch (Exception e) {
            logger.error("生成缩略图失败", e);
            return false;
        }
    }

    @Override
    public void createVideoCover(File sourceFile, int width, File targetFile) {
        try {
            // ffmpeg -i %s -y -vframes 1 -vf scale=%d:%d/a %s
            List<String> cmd = new ArrayList<>();
            cmd.add("ffmpeg");
            cmd.add("-i");
            cmd.add(sourceFile.getAbsolutePath());
            cmd.add("-y");
            cmd.add("-vframes");
            cmd.add("1");
            cmd.add("-vf");
            cmd.add("scale=" + width + ":" + width + "/a");
            cmd.add(targetFile.getAbsolutePath());

            executeCommand(cmd);
        } catch (Exception e) {
            logger.error("生成视频封面失败", e);
        }
    }

    @Override
    public void transcodeToTs(String sourceFilePath, String targetTsPath) {
        // ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(sourceFilePath);
        cmd.add("-vcodec");
        cmd.add("copy");
        cmd.add("-acodec");
        cmd.add("copy");
        cmd.add("-vbsf");
        cmd.add("h264_mp4toannexb");
        cmd.add(targetTsPath);

        try {
            executeCommand(cmd);
        } catch (Exception e) {
            logger.error("转码 TS 失败", e);
            throw new BusinessException("转码失败");
        }
    }

    @Override
    public void cutToM3u8(String sourceTsPath, String targetFolder, String fileId) {
        // ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-i");
        cmd.add(sourceTsPath);
        cmd.add("-c");
        cmd.add("copy");
        cmd.add("-map");
        cmd.add("0");
        cmd.add("-f");
        cmd.add("segment");
        cmd.add("-segment_list");
        cmd.add(targetFolder + "/index.m3u8"); 
        cmd.add("-segment_time");
        cmd.add("30");
        cmd.add(targetFolder + "/" + fileId + "_%4d.ts");

        try {
            executeCommand(cmd);
        } catch (Exception e) {
            logger.error("切割 M3U8 失败", e);
            throw new BusinessException("切片失败");
        }
    }

    private void executeCommand(List<String> cmd) throws BusinessException {
        if (cmd == null || cmd.isEmpty()) {
            return;
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true); // Merge stdout and stderr

        Process process = null;
        try {
            process = processBuilder.start();
            
            // Consume output to prevent blocking
            try (InputStream inputStream = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                while (reader.readLine() != null) {
                    // Consume output to prevent process blocking
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("FFmpeg process exited with code: " + exitCode);
                throw new BusinessException("视频处理失败");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing command: " + cmd, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("视频处理异常");
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
