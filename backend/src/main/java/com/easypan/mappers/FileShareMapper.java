package com.easypan.mappers;

import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.FileShare;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import com.mybatisflex.core.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分享数据库操作接口.
 */
@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {

    @Insert("<script>"
            + "<foreach collection='list' item='item' separator=';'>"
            + "INSERT INTO file_share "
            + "(share_id, user_id, file_id, share_time, expire_time, code, show_count, file_name, "
            + "folder_type, file_category, file_type, file_cover) "
            + "VALUES (#{item.shareId}, #{item.userId}, #{item.fileId}, "
            + "#{item.shareTime}, #{item.expireTime}, #{item.code}, #{item.showCount}, #{item.fileName}, "
            + "#{item.folderType}, #{item.fileCategory}, #{item.fileType}, #{item.fileCover}) "
            + "ON CONFLICT (share_id) DO UPDATE SET "
            + "expire_time = EXCLUDED.expire_time, code = EXCLUDED.code, "
            + "show_count = EXCLUDED.show_count, file_name = EXCLUDED.file_name, "
            + "folder_type = EXCLUDED.folder_type, file_category = EXCLUDED.file_category, "
            + "file_type = EXCLUDED.file_type, file_cover = EXCLUDED.file_cover"
            + "</foreach>"
            + "</script>")
    int insertOrUpdateBatch(@Param("list") java.util.List<FileShare> list);

    @Select("SELECT file_id, file_name, folder_type, file_category, file_type, file_cover "
            + "FROM file_info WHERE file_id = #{fileId}")
    FileInfo selectFileInfoByFileId(@Param("fileId") String fileId);

    @Update("UPDATE file_share SET show_count = show_count + 1 WHERE share_id = #{shareId}")
    void updateShareShowCount(@Param("shareId") String shareId);

    @Delete("<script>"
            + "DELETE FROM file_share WHERE user_id = #{userId} "
            + "AND share_id IN <foreach collection='shareIdArray' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>"
            + "</script>")
    Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray, @Param("userId") String userId);

    @Select("SELECT * FROM file_share WHERE user_id = #{userId} "
            + "AND (share_time, share_id) < (#{cursorTime}, #{cursorId}) "
            + "ORDER BY share_time DESC, share_id DESC LIMIT #{pageSize}")
    java.util.List<FileShare> selectByCursorPagination(
            @Param("userId") String userId,
            @Param("cursorTime") java.util.Date cursorTime,
            @Param("cursorId") String cursorId,
            @Param("pageSize") int pageSize);
}
