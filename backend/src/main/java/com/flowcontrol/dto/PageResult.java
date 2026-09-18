package com.flowcontrol.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果包装
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;

    public PageResult() {}

    public PageResult(List<T> records, long total, long size, long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long size, long current) {
        return new PageResult<>(records, total, size, current);
    }
}
