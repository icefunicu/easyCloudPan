package com.easypan.entity.query;

import com.easypan.entity.enums.PageSize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单分页类.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimplePage {
    private int pageNo;
    private int countTotal;
    private int pageSize;
    private int pageTotal;
    private int start;
    private int end;

    /**
     * 构造函数.
     *
     * @param pageNo 页码
     * @param countTotal 总数
     * @param pageSize 每页大小
     */
    public SimplePage(Integer pageNo, int countTotal, int pageSize) {
        if (null == pageNo) {
            pageNo = 0;
        }
        this.pageNo = pageNo;
        this.countTotal = countTotal;
        this.pageSize = pageSize;
        action();
    }

    /**
     * 构造函数.
     *
     * @param start 起始位置
     * @param end 结束位置
     */
    public SimplePage(int start, int end) {
        this.start = start;
        this.end = end;
    }

    /**
     * 计算分页参数.
     */
    public void action() {
        if (this.pageSize <= 0) {
            this.pageSize = PageSize.SIZE20.getSize();
        }
        if (this.countTotal > 0) {
            this.pageTotal = this.countTotal % this.pageSize == 0
                    ? this.countTotal / this.pageSize
                    : this.countTotal / this.pageSize + 1;
        } else {
            pageTotal = 1;
        }

        if (pageNo <= 1) {
            pageNo = 1;
        }
        if (pageNo > pageTotal) {
            pageNo = pageTotal;
        }
        this.start = (pageNo - 1) * pageSize;
        this.end = this.pageSize;
    }

    /**
     * 设置总数并重新计算分页参数.
     *
     * @param countTotal 总数
     */
    public void setCountTotal(int countTotal) {
        this.countTotal = countTotal;
        this.action();
    }
}
