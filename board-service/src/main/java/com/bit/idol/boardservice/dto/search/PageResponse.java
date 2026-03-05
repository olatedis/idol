package com.bit.idol.boardservice.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private boolean last;
    private int number;
    private int size;
}