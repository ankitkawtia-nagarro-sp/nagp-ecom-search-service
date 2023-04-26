package com.nagarro.nagp.search.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchQueryDto {
    private String textQuery;
    private List<Filter> filters;

    @Data
    public static class Filter implements Serializable {
        private String key;
        private List<String> value;
        private String from;
        private String to;
    }
}
