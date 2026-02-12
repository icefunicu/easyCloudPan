package com.easypan.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPage<T> {
    private java.util.List<T> list;
    private String nextCursor;
    private boolean hasMore;
    private int pageSize;
    private Long totalCount;

    public static <T> CursorPage<T> of(java.util.List<T> list, String nextCursor, int pageSize) {
        CursorPage<T> page = new CursorPage<>();
        page.setList(list);
        page.setNextCursor(nextCursor);
        page.setHasMore(nextCursor != null && !nextCursor.isEmpty());
        page.setPageSize(pageSize);
        return page;
    }

    public static <T> CursorPage<T> of(java.util.List<T> list, String nextCursor, int pageSize, Long totalCount) {
        CursorPage<T> page = of(list, nextCursor, pageSize);
        page.setTotalCount(totalCount);
        return page;
    }
}
