package com.easypan.task;

import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.query.SimplePage;
import com.easypan.service.FileInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件清理定时任务，清理回收站中过期的文件.
 */
@Component
public class FileCleanTask {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanTask.class);

    private static final int DEFAULT_BATCH_SIZE = 200;

    private static final int DEFAULT_MAX_ROUNDS = 2000;

    @Resource
    private FileInfoService fileInfoService;

    @Value("${app.file-clean.batch-size:" + DEFAULT_BATCH_SIZE + "}")
    private int batchSize;

    @Value("${app.file-clean.max-rounds:" + DEFAULT_MAX_ROUNDS + "}")
    private int maxRounds;

    /**
     * 执行文件清理任务.
     */
    @Scheduled(fixedDelayString = "${app.file-clean.fixed-delay-ms:180000}")
    public void execute() {
        int effectiveBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        int effectiveMaxRounds = maxRounds > 0 ? maxRounds : DEFAULT_MAX_ROUNDS;

        int rounds = 0;
        int totalCleaned = 0;
        long startTime = System.currentTimeMillis();

        while (rounds < effectiveMaxRounds) {
            List<FileInfo> fileInfoList = loadExpiredRecycleBatch(effectiveBatchSize);
            if (fileInfoList.isEmpty()) {
                break;
            }

            totalCleaned += cleanBatch(fileInfoList);
            rounds++;

            // If this round returned less than one full batch, no more rows are expected.
            if (fileInfoList.size() < effectiveBatchSize) {
                break;
            }
        }

        long costMs = System.currentTimeMillis() - startTime;
        if (rounds >= effectiveMaxRounds) {
            logger.warn("FileCleanTask reached max rounds. rounds={}, batchSize={}", rounds, effectiveBatchSize);
        }
        if (totalCleaned > 0) {
            logger.info("FileCleanTask completed. cleanedFiles={}, rounds={}, costMs={}", totalCleaned, rounds, costMs);
        }
    }

    private List<FileInfo> loadExpiredRecycleBatch(int currentBatchSize) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        fileInfoQuery.setQueryExpire(true);
        fileInfoQuery.setOrderBy("recovery_time asc");
        SimplePage simplePage = new SimplePage();
        simplePage.setStart(0);
        simplePage.setPageSize(currentBatchSize);
        fileInfoQuery.setSimplePage(simplePage);
        return fileInfoService.findListByParam(fileInfoQuery);
    }

    private int cleanBatch(List<FileInfo> fileInfoList) {
        int cleaned = 0;
        Map<String, List<FileInfo>> fileInfoMap = fileInfoList.stream()
                .collect(Collectors.groupingBy(FileInfo::getUserId));
        for (Map.Entry<String, List<FileInfo>> entry : fileInfoMap.entrySet()) {
            List<String> fileIds = entry.getValue().stream().map(p -> p.getFileId()).collect(Collectors.toList());
            fileInfoService.delFileBatch(entry.getKey(), String.join(",", fileIds), false);
            cleaned += fileIds.size();
        }
        return cleaned;
    }
}
