-- Web Vitals Performance Metrics Table
CREATE TABLE IF NOT EXISTS web_vitals_metrics (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metric_name VARCHAR(20) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    rating VARCHAR(20) NOT NULL,
    page_url VARCHAR(500),
    user_agent VARCHAR(500),
    user_id BIGINT,
    session_id VARCHAR(100),
    country VARCHAR(50),
    device_type VARCHAR(20),
    connection_type VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN web_vitals_metrics.metric_name IS 'Metric name: LCP, INP, CLS, FCP, TTFB';
COMMENT ON COLUMN web_vitals_metrics.metric_value IS 'Metric value in ms or score';
COMMENT ON COLUMN web_vitals_metrics.rating IS 'Rating: good, needs-improvement, poor';
COMMENT ON COLUMN web_vitals_metrics.page_url IS 'Page URL where metric was collected';
COMMENT ON COLUMN web_vitals_metrics.user_agent IS 'Browser user agent';
COMMENT ON COLUMN web_vitals_metrics.user_id IS 'User ID if logged in';
COMMENT ON COLUMN web_vitals_metrics.session_id IS 'Session identifier';
COMMENT ON COLUMN web_vitals_metrics.country IS 'Country from IP';
COMMENT ON COLUMN web_vitals_metrics.device_type IS 'Device type: desktop, mobile, tablet';
COMMENT ON COLUMN web_vitals_metrics.connection_type IS 'Connection type: 4g, 3g, wifi, etc';
COMMENT ON COLUMN web_vitals_metrics.created_at IS 'Timestamp when metric was recorded';
COMMENT ON TABLE web_vitals_metrics IS 'Web Vitals performance metrics storage';

CREATE INDEX IF NOT EXISTS idx_wv_metric_name ON web_vitals_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_wv_rating ON web_vitals_metrics(rating);
CREATE INDEX IF NOT EXISTS idx_wv_created_at ON web_vitals_metrics(created_at);
CREATE INDEX IF NOT EXISTS idx_wv_user_id ON web_vitals_metrics(user_id);
CREATE INDEX IF NOT EXISTS idx_wv_device_type ON web_vitals_metrics(device_type);

-- Performance Aggregation Summary Table (for daily/hourly stats)
CREATE TABLE IF NOT EXISTS performance_summary (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metric_name VARCHAR(20) NOT NULL,
    period_type VARCHAR(10) NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    sample_count INT NOT NULL DEFAULT 0,
    avg_value DOUBLE PRECISION,
    median_value DOUBLE PRECISION,
    p75_value DOUBLE PRECISION,
    p95_value DOUBLE PRECISION,
    good_count INT DEFAULT 0,
    needs_improvement_count INT DEFAULT 0,
    poor_count INT DEFAULT 0,
    device_type VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN performance_summary.metric_name IS 'Metric name';
COMMENT ON COLUMN performance_summary.period_type IS 'hourly, daily, weekly';
COMMENT ON COLUMN performance_summary.period_start IS 'Start of the aggregation period';
COMMENT ON COLUMN performance_summary.period_end IS 'End of the aggregation period';
COMMENT ON COLUMN performance_summary.sample_count IS 'Number of samples in this period';
COMMENT ON COLUMN performance_summary.avg_value IS 'Average metric value';
COMMENT ON COLUMN performance_summary.median_value IS 'Median metric value';
COMMENT ON COLUMN performance_summary.p75_value IS '75th percentile value';
COMMENT ON COLUMN performance_summary.p95_value IS '95th percentile value';
COMMENT ON COLUMN performance_summary.good_count IS 'Count of good ratings';
COMMENT ON COLUMN performance_summary.needs_improvement_count IS 'Count of needs-improvement ratings';
COMMENT ON COLUMN performance_summary.poor_count IS 'Count of poor ratings';
COMMENT ON COLUMN performance_summary.device_type IS 'Device type if segmented';
COMMENT ON TABLE performance_summary IS 'Aggregated performance statistics';

CREATE UNIQUE INDEX IF NOT EXISTS idx_ps_unique_period ON performance_summary(metric_name, period_type, period_start, device_type);
CREATE INDEX IF NOT EXISTS idx_ps_period_start ON performance_summary(period_start);

-- Trigger for auto-updating updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Drop trigger if exists before creating (for idempotency)
DROP TRIGGER IF EXISTS update_performance_summary_updated_at ON performance_summary;
CREATE TRIGGER update_performance_summary_updated_at
    BEFORE UPDATE ON performance_summary
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
