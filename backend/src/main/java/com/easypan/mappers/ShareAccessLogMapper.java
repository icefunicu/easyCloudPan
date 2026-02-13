package com.easypan.mappers;

import com.easypan.entity.po.ShareAccessLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;

/**
 * 分享访问日志数据库操作接口.
 */
@Mapper
public interface ShareAccessLogMapper extends BaseMapper<ShareAccessLog> {

    @Insert("INSERT INTO share_access_log (share_id, file_id, visitor_id, visitor_ip, visitor_user_agent, "
            + "access_type, access_time, success, error_message) "
            + "VALUES (#{shareId}, #{fileId}, #{visitorId}, #{visitorIp}, #{visitorUserAgent}, "
            + "#{accessType}, #{accessTime}, #{success}, #{errorMessage})")
    int insertLog(ShareAccessLog log);
}
