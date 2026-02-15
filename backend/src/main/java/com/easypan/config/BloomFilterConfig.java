package com.easypan.config;

import com.easypan.mappers.FileInfoMapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 布隆过滤器配置.
 *
 *<p>用于缓存已存在的文件 MD5，减少秒传场景下对数据库的无效访问。
 */
@Configuration
public class BloomFilterConfig implements SmartInitializingSingleton {

    private static final long EXPECTED_INSERTIONS = 10_000_000L;
    private static final double FPP = 0.0001;

    @Resource
    private FileInfoMapper fileInfoMapper;

    /**
     * 创建文件 MD5 布隆过滤器.
     *
     * @return 布隆过滤器实例
     */
    @Bean("fileMd5BloomFilter")
    @SuppressWarnings("null")
    public BloomFilter<String> fileMd5BloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP);
    }

    /**
     * 创建用户 ID 布隆过滤器.
     * 用于快速判断用户是否存在.
     */
    @Bean("userIdBloomFilter")
    @SuppressWarnings("null")
    public BloomFilter<String> userIdBloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                100_000,
                0.01);
    }

    /**
     * 创建 Token 黑名单布隆过滤器.
     */
    @Bean("tokenBloomFilter")
    @SuppressWarnings("null")
    public BloomFilter<String> tokenBloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                1_000_000,
                0.01);
    }

    /**
     * 应用启动后初始化布隆过滤器，将已有文件 MD5 预热进去.
     * 使用 SmartInitializingSingleton 确保所有单例 bean 初始化完成后再执行.
     */
    @Override
    public void afterSingletonsInstantiated() {
        List<String> allMd5 = fileInfoMapper.selectAllMd5();
        if (allMd5 != null && !allMd5.isEmpty()) {
            BloomFilter<String> bloomFilter = fileMd5BloomFilter();
            allMd5.forEach(bloomFilter::put);
        }
    }
}
