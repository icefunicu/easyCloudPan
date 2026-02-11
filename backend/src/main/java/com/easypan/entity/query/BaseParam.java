package com.easypan.entity.query;

import lombok.Data;

/**
 * 基础查询参数
 */
@Data
public class BaseParam {
    private SimplePage simplePage;
    private Integer pageNo;
    private Integer pageSize;
    private String orderBy;
    
    /**
     * 获取分页大小，如果未设置则返回默认值 15
     */
    public Integer getPageSize() {
        return pageSize != null ? pageSize : 15;
    }
    
    /**
     * 获取页码，如果未设置则返回默认值 1
     */
    public Integer getPageNo() {
        return pageNo != null ? pageNo : 1;
    }
}
