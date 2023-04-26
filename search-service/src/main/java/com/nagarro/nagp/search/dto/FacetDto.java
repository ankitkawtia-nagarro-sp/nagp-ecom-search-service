package com.nagarro.nagp.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacetDto {
    private String facetName;
    private List<FacetValueDto> facetValueDto;
}
