package com.easypan.mappers;

import com.easypan.entity.po.EmailCode;
import org.apache.ibatis.annotations.Param;
import com.mybatisflex.core.BaseMapper;

import java.util.List;

/**
 * 邮箱验证码 数据库操作接口
 */
public interface EmailCodeMapper extends BaseMapper<EmailCode> {

    /**
     * Custom selectList for legacy compatibility
     */
    List<EmailCode> selectList(@Param("query") Object query);

    /**
     * Custom selectCount for legacy compatibility
     */
    Integer selectCount(@Param("query") Object query);

    /**
     * Batch insert or update
     */
    Integer insertOrUpdateBatch(@Param("list") List<EmailCode> list);

    /**
     * 根据EmailAndCode更新
     */
    Integer updateByEmailAndCode(@Param("bean") EmailCode t, @Param("email") String email, @Param("code") String code);

    /**
     * 根据EmailAndCode删除
     */
    Integer deleteByEmailAndCode(@Param("email") String email, @Param("code") String code);

    /**
     * 根据EmailAndCode获取对象
     */
    EmailCode selectByEmailAndCode(@Param("email") String email, @Param("code") String code);

    void disableEmailCode(@Param("email") String email);

}
