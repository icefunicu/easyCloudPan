package com.easypan.mappers;

import com.easypan.entity.po.FileShare;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
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
            + "(share_id, user_id, file_id, share_time, expire_time, code, show_count, file_name) "
            + "VALUES (#{item.shareId}, #{item.userId}, #{item.fileId}, "
            + "#{item.shareTime}, #{item.expireTime}, #{item.code}, #{item.showCount}, #{item.fileName}) "
            + "ON DUPLICATE KEY UPDATE "
            + "expire_time = VALUES(expire_time), code = VALUES(code), "
            + "show_count = VALUES(show_count), file_name = VALUES(file_name)"
            + "</foreach>"
            + "</script>")
    int insertOrUpdateBatch(@Param("list") java.util.List<FileShare> list);

    @Update("UPDATE file_share SET show_count = show_count + 1 WHERE share_id = #{shareId}")
    void updateShareShowCount(@Param("shareId") String shareId);

    @Delete("<script>"
            + "DELETE FROM file_share WHERE user_id = #{userId} "
            + "AND share_id IN <foreach collection='shareIdArray' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>"
            + "</script>")
    Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray, @Param("userId") String userId);

}
