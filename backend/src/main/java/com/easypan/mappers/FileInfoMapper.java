package com.easypan.mappers;

import com.easypan.entity.po.FileInfo;
import org.apache.ibatis.annotations.Param;
import com.mybatisflex.core.BaseMapper;

import java.util.List;

/**
 * 文件信息 数据库操作接口
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {

        /**
         * Custom selectList for legacy compatibility
         */
        List<FileInfo> selectList(@Param("query") Object query);

        /**
         * Custom selectCount for legacy compatibility
         */
        Integer selectCount(@Param("query") Object query);

        /**
         * Batch insert or update
         */
        Integer insertOrUpdateBatch(@Param("list") List<FileInfo> list);

        /**
         * 根据FileIdAndUserId更新
         */
        Integer updateByFileIdAndUserId(@Param("bean") FileInfo t, @Param("fileId") String fileId,
                        @Param("userId") String userId);

        /**
         * 根据FileIdAndUserId删除
         */
        Integer deleteByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

        /**
         * 根据FileIdAndUserId获取对象
         */
        FileInfo selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

        void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") String userId,
                        @Param("bean") FileInfo t,
                        @Param("oldStatus") Integer oldStatus);

        void updateFileDelFlagBatch(@Param("bean") FileInfo fileInfo,
                        @Param("userId") String userId,
                        @Param("filePidList") List<String> filePidList,
                        @Param("fileIdList") List<String> fileIdList,
                        @Param("oldDelFlag") Integer oldDelFlag);

        void delFileBatch(@Param("userId") String userId,
                        @Param("filePidList") List<String> filePidList,
                        @Param("fileIdList") List<String> fileIdList,
                        @Param("oldDelFlag") Integer oldDelFlag);

        Long selectUseSpace(@Param("userId") String userId);

        void deleteFileByUserId(@Param("userId") String userId);

        List<String> selectDescendantFolderIds(@Param("fileIdList") List<String> fileIdList,
                        @Param("userId") String userId,
                        @Param("delFlag") Integer delFlag);
}
