package com.easypan.service.impl;

import com.easypan.exception.BusinessException;
import com.easypan.service.MediaTranscodeService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 媒体转码服务实现类.
 */
@Service("mediaTranscodeService")
public class MediaTranscodeServiceImpl implements MediaTranscodeService {

    private static final Logger logger = LoggerFactory.getLogger(MediaTranscodeServiceImpl.class);

    private static final String FFMPEG_PATH;

    static {
        String path = "";
        try {
            path = new ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator().getExecutablePath();
        } catch (Exception e) {
            logger.error("Failed to resolve FFmpeg path", e);
        }
        FFMPEG_PATH = path;
    }

    @Override
    public boolean createThumbnail(File sourceFile, int width, File targetFile, boolean delSource) {
        try {
            // 使用 Thumbnailator 生成缩略图，不需要 FFmpeg
            net.coobird.thumbnailator.Thumbnails.of(sourceFile)
                    .width(width)
                    .toFile(targetFile);

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
            List<String> cmd = new ArrayList<>();
            cmd.add(FFMPEG_PATH);
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
        List<String> cmd = new ArrayList<>();
        // 使用 JAVE2 获取内嵌 FFmpeg 路径
        cmd.add(FFMPEG_PATH);
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
        List<String> cmd = new ArrayList<>();
        // 使用 JAVE2 获取内嵌 FFmpeg 路径
        cmd.add(FFMPEG_PATH);
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
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            process = processBuilder.start();

            try (InputStream inputStream = process.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("FFmpeg output: {}", line);
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
