package com.easypan.mappers;

import com.easypan.entity.po.FileInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.mybatisflex.core.BaseMapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息 数据库操作接口
 */
@Mapper
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

        /**
         * 游标分页查询文件列表
         * 使用 (create_time, file_id) 作为游标，性能优于 OFFSET 分页
         *
         * @param userId     用户ID
         * @param cursorTime 游标时间（上一页最后一条记录的创建时间）
         * @param cursorId   游标ID（上一页最后一条记录的文件ID）
         * @param pageSize   每页数量
         * @return 文件列表
         */
        @Select("SELECT * FROM file_info WHERE user_id = #{userId} " +
                        "AND (create_time, file_id) < (#{cursorTime}, #{cursorId}) " +
                        "ORDER BY create_time DESC, file_id DESC LIMIT #{pageSize}")
        List<FileInfo> selectByCursorPagination(
                        @Param("userId") String userId,
                        @Param("cursorTime") Date cursorTime,
                        @Param("cursorId") String cursorId,
                        @Param("pageSize") int pageSize);

        /**
         * 根据 MD5 和状态查询单个文件（秒传优化）
         * 使用覆盖索引加 LIMIT 1，避免全表扫描
         */
        @Select("SELECT file_id, file_size, file_path, file_md5, user_id, file_cover " +
                        "FROM file_info " +
                        "WHERE file_md5 = #{fileMd5} AND status = #{status} " +
                        "LIMIT 1")
        FileInfo selectOneByMd5AndStatus(@Param("fileMd5") String fileMd5,
                        @Param("status") Integer status);

        /**
         * 查询所有已存在的文件 MD5，用于初始化布隆过滤器。
         */
        @Select("SELECT DISTINCT file_md5 FROM file_info WHERE file_md5 IS NOT NULL")
        List<String> selectAllMd5();

}
