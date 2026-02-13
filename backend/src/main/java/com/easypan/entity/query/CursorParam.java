package com.easypan.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游标分页参数类.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorParam {
    private String cursor;
    private Integer pageSize = 20;

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 获取有效的每页大小.
     *
     * @return 有效的每页大小
     */
    public int getEffectivePageSize() {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
