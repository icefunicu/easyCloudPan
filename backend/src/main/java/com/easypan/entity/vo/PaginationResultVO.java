package com.easypan.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResultVO<T> {
	private Integer totalCount;
	private Integer pageSize;
	private Integer pageNo;
	private Integer pageTotal;
	private List<T> list = new ArrayList<T>();

	public PaginationResultVO(Integer totalCount, Integer pageSize, Integer pageNo, List<T> list) {
		this.totalCount = totalCount;
		this.pageSize = pageSize;
		this.pageNo = pageNo;
		this.list = list;
	}

	public PaginationResultVO(List<T> list) {
		this.list = list;
	}

	// Custom setter for pageNo logic or custom constructor if needed
	// The original had a check in the 5-args constructor
	public static <T> PaginationResultVO<T> build(Integer totalCount, Integer pageSize, Integer pageNo,
			Integer pageTotal, List<T> list) {
		if (pageNo == null || pageNo == 0) {
			pageNo = 1;
		}
		return new PaginationResultVO<>(totalCount, pageSize, pageNo, pageTotal, list);
	}
}
