package com.easypan.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游标分页结果类.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPage<T> {
    private List<T> list;
    private String nextCursor;
    private boolean hasMore;
    private int pageSize;
    private Long totalCount;

    /**
     * 创建游标分页结果.
     *
     * @param list 数据列表
     * @param nextCursor 下一页游标
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public static <T> CursorPage<T> of(List<T> list, String nextCursor, int pageSize) {
        CursorPage<T> page = new CursorPage<>();
        page.setList(list);
        page.setNextCursor(nextCursor);
        page.setHasMore(nextCursor != null && !nextCursor.isEmpty());
        page.setPageSize(pageSize);
        return page;
    }

    /**
     * 创建游标分页结果（包含总数）.
     *
     * @param list 数据列表
     * @param nextCursor 下一页游标
     * @param pageSize 每页大小
     * @param totalCount 总数
     * @return 分页结果
     */
    public static <T> CursorPage<T> of(
            List<T> list, String nextCursor, int pageSize, Long totalCount) {
        CursorPage<T> page = of(list, nextCursor, pageSize);
        page.setTotalCount(totalCount);
        return page;
    }
}
