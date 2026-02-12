package com.easypan.mappers;

import com.easypan.entity.po.ShareAccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface ShareAccessLogMapper {

    @Insert("INSERT INTO share_access_log (share_id, file_id, visitor_id, visitor_ip, visitor_user_agent, access_type, access_time, success, error_message) " +
            "VALUES (#{shareId}, #{fileId}, #{visitorId}, #{visitorIp}, #{visitorUserAgent}, #{accessType}, #{accessTime}, #{success}, #{errorMessage})")
    int insert(ShareAccessLog log);
}
