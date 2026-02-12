package com.easypan.config;

import com.easypan.mappers.FileInfoMapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 布隆过滤器配置
 *
 * 用于缓存已存在的文件 MD5，减少秒传场景下对数据库的无效访问。
 * 对应任务：
 *  - 5.1.1 集成 Guava BloomFilter
 *  - 5.1.2 创建 BloomFilterConfig 配置类
 *  - 5.1.3 在启动时初始化布隆过滤器
 */
@Configuration
public class BloomFilterConfig {

    private static final long EXPECTED_INSERTIONS = 10_000_000L;
    private static final double FPP = 0.0001; // 0.01% 误判率

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private BloomFilter<String> fileMd5BloomFilter;

    @Bean
    public BloomFilter<String> fileMd5BloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP
        );
    }

    /**
     * 应用启动后初始化布隆过滤器，将已有文件 MD5 预热进去。
     */
    @PostConstruct
    public void initBloomFilter() {
        List<String> allMd5 = fileInfoMapper.selectAllMd5();
        if (allMd5 != null && !allMd5.isEmpty()) {
            allMd5.forEach(fileMd5BloomFilter::put);
        }
    }
}

