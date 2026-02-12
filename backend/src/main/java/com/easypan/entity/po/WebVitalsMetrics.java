package com.easypan.entity.po;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("web_vitals_metrics")
public class WebVitalsMetrics implements Serializable {
    
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    private String metricName;
    
    private Double metricValue;
    
    private String rating;
    
    private String pageUrl;
    
    private String userAgent;
    
    private Long userId;
    
    private String sessionId;
    
    private String country;
    
    private String deviceType;
    
    private String connectionType;
    
    private LocalDateTime createdAt;
}
