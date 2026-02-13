package com.easypan.entity.query;

import lombok.Data;

/**
 * 基础查询参数类.
 */
@Data
public class BaseParam {
    private SimplePage simplePage;
    private Integer pageNo;
    private Integer pageSize;
    private String orderBy;

    /**
     * 获取分页大小，如果未设置则返回默认值 15.
     *
     * @return 分页大小
     */
    public Integer getPageSize() {
        return pageSize != null ? pageSize : 15;
    }

    /**
     * 获取页码，如果未设置则返回默认值 1.
     *
     * @return 页码
     */
    public Integer getPageNo() {
        return pageNo != null ? pageNo : 1;
    }
}
