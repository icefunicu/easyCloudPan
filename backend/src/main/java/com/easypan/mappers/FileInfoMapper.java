package com.easypan.mappers;

import com.easypan.entity.po.FileInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import com.mybatisflex.core.BaseMapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息数据库操作接口.
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    @Insert("<script>"
            + "<foreach collection='list' item='item' separator=';'>"
            + "INSERT INTO file_info (file_id, user_id, file_md5, file_pid, file_name, file_path, "
            + "file_size, file_cover, file_category, file_type, folder_type, status, del_flag, "
            + "recovery_time, create_time, last_update_time) "
            + "VALUES (#{item.fileId}, #{item.userId}, #{item.fileMd5}, #{item.filePid}, "
            + "#{item.fileName}, #{item.filePath}, #{item.fileSize}, #{item.fileCover}, "
            + "#{item.fileCategory}, #{item.fileType}, #{item.folderType}, #{item.status}, "
            + "#{item.delFlag}, #{item.recoveryTime}, #{item.createTime}, #{item.lastUpdateTime}) "
            + "ON DUPLICATE KEY UPDATE "
            + "file_pid = VALUES(file_pid), file_name = VALUES(file_name), file_path = VALUES(file_path), "
            + "file_size = VALUES(file_size), file_cover = VALUES(file_cover), file_category = VALUES(file_category), "
            + "file_type = VALUES(file_type), folder_type = VALUES(folder_type), status = VALUES(status), "
            + "del_flag = VALUES(del_flag), recovery_time = VALUES(recovery_time), last_update_time = VALUES(last_update_time)"
            + "</foreach>"
            + "</script>")
    int insertOrUpdateBatch(@Param("list") List<FileInfo> list);

    @Update("UPDATE file_info "
            + "SET status = #{bean.status}, "
            + "file_size = #{bean.fileSize}, "
            + "file_cover = #{bean.fileCover}, "
            + "recovery_time = COALESCE(#{bean.recoveryTime}, recovery_time), "
            + "last_update_time = CURRENT_TIMESTAMP "
            + "WHERE file_id = #{fileId} AND user_id = #{userId} AND status = #{oldStatus}")
    void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") String userId,
            @Param("bean") FileInfo t, @Param("oldStatus") Integer oldStatus);

    @Update("<script>"
            + "UPDATE file_info SET del_flag = #{bean.delFlag}, recovery_time = #{bean.recoveryTime} "
            + "WHERE user_id = #{userId} AND del_flag = #{oldDelFlag} "
            + "<if test='filePidList != null and filePidList.size() > 0'>"
            + "AND file_pid IN <foreach collection='filePidList' item='pid' open='(' separator=',' close=')'>#{pid}</foreach>"
            + "</if>"
            + "<if test='fileIdList != null and fileIdList.size() > 0'>"
            + "AND file_id IN <foreach collection='fileIdList' item='fid' open='(' separator=',' close=')'>#{fid}</foreach>"
            + "</if>"
            + "</script>")
    void updateFileDelFlagBatch(@Param("bean") FileInfo fileInfo,
            @Param("userId") String userId,
            @Param("filePidList") List<String> filePidList,
            @Param("fileIdList") List<String> fileIdList,
            @Param("oldDelFlag") Integer oldDelFlag);

    @Delete("<script>"
            + "DELETE FROM file_info WHERE user_id = #{userId} AND del_flag = #{oldDelFlag} "
            + "<if test='filePidList != null and filePidList.size() > 0'>"
            + "AND file_pid IN <foreach collection='filePidList' item='pid' open='(' separator=',' close=')'>#{pid}</foreach>"
            + "</if>"
            + "<if test='fileIdList != null and fileIdList.size() > 0'>"
            + "AND file_id IN <foreach collection='fileIdList' item='fid' open='(' separator=',' close=')'>#{fid}</foreach>"
            + "</if>"
            + "</script>")
    void delFileBatch(@Param("userId") String userId,
            @Param("filePidList") List<String> filePidList,
            @Param("fileIdList") List<String> fileIdList,
            @Param("oldDelFlag") Integer oldDelFlag);

    @Select("SELECT COALESCE(SUM(file_size), 0) FROM file_info WHERE user_id = #{userId} AND del_flag != 0")
    Long selectUseSpace(@Param("userId") String userId);

    @Delete("DELETE FROM file_info WHERE user_id = #{userId}")
    void deleteFileByUserId(@Param("userId") String userId);

    @Select("<script>"
            + "WITH RECURSIVE descendants AS ("
            + "SELECT file_id FROM file_info WHERE user_id = #{userId} AND del_flag = #{delFlag} "
            + "AND file_id IN <foreach collection='fileIdList' item='fid' open='(' separator=',' close=')'>#{fid}</foreach> "
            + "UNION ALL "
            + "SELECT f.file_id FROM file_info f "
            + "INNER JOIN descendants d ON f.file_pid = d.file_id "
            + "WHERE f.user_id = #{userId} AND f.del_flag = #{delFlag}"
            + ") SELECT file_id FROM descendants"
            + "</script>")
    List<String> selectDescendantFolderIds(@Param("fileIdList") List<String> fileIdList,
            @Param("userId") String userId,
            @Param("delFlag") Integer delFlag);

    @Select("SELECT * FROM file_info WHERE user_id = #{userId} "
            + "AND (create_time, file_id) < (#{cursorTime}, #{cursorId}) "
            + "ORDER BY create_time DESC, file_id DESC LIMIT #{pageSize}")
    List<FileInfo> selectByCursorPagination(
            @Param("userId") String userId,
            @Param("cursorTime") Date cursorTime,
            @Param("cursorId") String cursorId,
            @Param("pageSize") int pageSize);

    @Select("SELECT file_id, file_size, file_path, file_md5, user_id, file_cover "
            + "FROM file_info "
            + "WHERE file_md5 = #{fileMd5} AND status = #{status} "
            + "LIMIT 1")
    FileInfo selectOneByMd5AndStatus(@Param("fileMd5") String fileMd5,
            @Param("status") Integer status);

    @Select("SELECT DISTINCT file_md5 FROM file_info WHERE file_md5 IS NOT NULL")
    List<String> selectAllMd5();

}
