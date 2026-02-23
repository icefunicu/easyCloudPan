package com.easypan.mappers;

import com.easypan.entity.po.EmailCode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import com.mybatisflex.core.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 邮箱验证码数据库操作接口.
 */
@Mapper
public interface EmailCodeMapper extends BaseMapper<EmailCode> {

    @Insert("<script>"
            + "<foreach collection='list' item='item' separator=';'>"
            + "INSERT INTO email_code (email, code, create_time, status) "
            + "VALUES (#{item.email}, #{item.code}, #{item.createTime}, #{item.status}) "
            + "ON CONFLICT (email, code) DO UPDATE SET "
            + "create_time = EXCLUDED.create_time, status = EXCLUDED.status"
            + "</foreach>"
            + "</script>")
    int insertOrUpdateBatch(@Param("list") java.util.List<EmailCode> list);

    @Update("UPDATE email_code SET status = 1 WHERE email = #{email}")
    void disableEmailCode(@Param("email") String email);

}
