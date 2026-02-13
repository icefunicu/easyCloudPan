package com.easypan.entity.mapper;

import com.mybatisflex.core.BaseMapper;
import com.easypan.entity.po.WebVitalsMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Web Vitals 指标数据库操作接口.
 */
@Mapper
public interface WebVitalsMetricsMapper extends BaseMapper<WebVitalsMetrics> {

    @Select("SELECT metric_name, rating, COUNT(*) as count, AVG(metric_value) as avg_value "
            + "FROM web_vitals_metrics "
            + "WHERE created_at >= #{startTime} AND created_at <= #{endTime} "
            + "GROUP BY metric_name, rating")
    List<Map<String, Object>> getMetricsSummary(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("SELECT metric_name, "
            + "AVG(metric_value) as avg_value, "
            + "MIN(metric_value) as min_value, "
            + "MAX(metric_value) as max_value, "
            + "COUNT(*) as sample_count "
            + "FROM web_vitals_metrics "
            + "WHERE created_at >= #{startTime} AND created_at <= #{endTime} "
            + "AND metric_name = #{metricName} "
            + "GROUP BY metric_name")
    Map<String, Object> getMetricStats(@Param("metricName") String metricName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
