-- Web Vitals Performance Metrics Table
CREATE TABLE IF NOT EXISTS web_vitals_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(20) NOT NULL COMMENT 'Metric name: LCP, INP, CLS, FCP, TTFB',
    metric_value DOUBLE NOT NULL COMMENT 'Metric value in ms or score',
    rating VARCHAR(20) NOT NULL COMMENT 'Rating: good, needs-improvement, poor',
    page_url VARCHAR(500) COMMENT 'Page URL where metric was collected',
    user_agent VARCHAR(500) COMMENT 'Browser user agent',
    user_id BIGINT COMMENT 'User ID if logged in',
    session_id VARCHAR(100) COMMENT 'Session identifier',
    country VARCHAR(50) COMMENT 'Country from IP',
    device_type VARCHAR(20) COMMENT 'Device type: desktop, mobile, tablet',
    connection_type VARCHAR(20) COMMENT 'Connection type: 4g, 3g, wifi, etc',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when metric was recorded',
    
    INDEX idx_metric_name (metric_name),
    INDEX idx_rating (rating),
    INDEX idx_created_at (created_at),
    INDEX idx_user_id (user_id),
    INDEX idx_device_type (device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Web Vitals performance metrics storage';

-- Performance Aggregation Summary Table (for daily/hourly stats)
CREATE TABLE IF NOT EXISTS performance_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(20) NOT NULL,
    period_type VARCHAR(10) NOT NULL COMMENT 'hourly, daily, weekly',
    period_start TIMESTAMP NOT NULL COMMENT 'Start of the aggregation period',
    period_end TIMESTAMP NOT NULL COMMENT 'End of the aggregation period',
    sample_count INT NOT NULL DEFAULT 0 COMMENT 'Number of samples in this period',
    avg_value DOUBLE COMMENT 'Average metric value',
    median_value DOUBLE COMMENT 'Median metric value',
    p75_value DOUBLE COMMENT '75th percentile value',
    p95_value DOUBLE COMMENT '95th percentile value',
    good_count INT DEFAULT 0 COMMENT 'Count of good ratings',
    needs_improvement_count INT DEFAULT 0 COMMENT 'Count of needs-improvement ratings',
    poor_count INT DEFAULT 0 COMMENT 'Count of poor ratings',
    device_type VARCHAR(20) COMMENT 'Device type if segmented',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE INDEX idx_unique_period (metric_name, period_type, period_start, device_type),
    INDEX idx_period_start (period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Aggregated performance statistics';
