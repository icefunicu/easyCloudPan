package com.easypan.mappers;

import com.easypan.entity.po.FileShare;
import org.apache.ibatis.annotations.Param;
import com.mybatisflex.core.BaseMapper;

import java.util.List;

/**
 * 分享信息 数据库操作接口
 */
public interface FileShareMapper extends BaseMapper<FileShare> {

    /**
     * Custom selectList for legacy compatibility
     */
    List<FileShare> selectList(@Param("query") Object query);

    /**
     * Custom selectCount for legacy compatibility
     */
    Integer selectCount(@Param("query") Object query);

    /**
     * Batch insert or update
     */
    Integer insertOrUpdateBatch(@Param("list") List<FileShare> list);

    /**
     * 根据ShareId更新
     */
    Integer updateByShareId(@Param("bean") FileShare t, @Param("shareId") String shareId);

    /**
     * 根据ShareId删除
     */
    Integer deleteByShareId(@Param("shareId") String shareId);

    /**
     * 根据ShareId获取对象
     */
    FileShare selectByShareId(@Param("shareId") String shareId);

    Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray, @Param("userId") String userId);

    void updateShareShowCount(@Param("shareId") String shareId);
}
