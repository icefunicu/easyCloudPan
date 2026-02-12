package com.easypan.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 用户信息 数据库操作接口
 */
import com.mybatisflex.core.BaseMapper;
import com.easypan.entity.po.UserInfo;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息 数据库操作接口
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

        /**
         * Custom selectList for legacy compatibility
         */
        List<UserInfo> selectList(@Param("query") Object query);

        /**
         * Custom selectCount for legacy compatibility
         */
        Integer selectCount(@Param("query") Object query);

        /**
         * Batch insert or update
         */
        Integer insertOrUpdateBatch(@Param("list") List<UserInfo> list);

        /**
         * 根据UserId更新
         */
        Integer updateByUserId(@Param("bean") UserInfo t, @Param("userId") String userId);

        /**
         * 根据UserId删除
         */
        Integer deleteByUserId(@Param("userId") String userId);

        /**
         * 根据UserId获取对象
         */
        UserInfo selectByUserId(@Param("userId") String userId);

        /**
         * 根据Email更新
         */
        Integer updateByEmail(@Param("bean") UserInfo t, @Param("email") String email);

        /**
         * 根据Email删除
         */
        Integer deleteByEmail(@Param("email") String email);

        /**
         * 根据Email获取对象
         */
        UserInfo selectByEmail(@Param("email") String email);

        /**
         * 根据NickName更新
         */
        Integer updateByNickName(@Param("bean") UserInfo t, @Param("nickName") String nickName);

        /**
         * 根据NickName删除
         */
        Integer deleteByNickName(@Param("nickName") String nickName);

        /**
         * 根据NickName获取对象
         */
        UserInfo selectByNickName(@Param("nickName") String nickName);

        /**
         * 根据QqOpenId更新
         */
        Integer updateByQqOpenId(@Param("bean") UserInfo t, @Param("qqOpenId") String qqOpenId);

        /**
         * 根据QqOpenId删除
         */
        Integer deleteByQqOpenId(@Param("qqOpenId") String qqOpenId);

        /**
         * 根据QqOpenId获取对象
         */
        UserInfo selectByQqOpenId(@Param("qqOpenId") String qqOpenId);

        Integer updateUserSpace(@Param("userId") String userId, @Param("useSpace") Long useSpace,
                        @Param("totalSpace") Long totalSpace);

        Integer updateUserSpaceAdmin(@Param("userId") String userId, @Param("useSpace") Long useSpace,
                        @Param("totalSpace") Long totalSpace);

        /**
         * 查询活跃用户（按最后登录时间排序）
         * 用于缓存预热
         * 
         * @param limit 限制数量
         * @return 活跃用户列表
         */
        List<UserInfo> selectActiveUsers(@Param("limit") int limit);

}
