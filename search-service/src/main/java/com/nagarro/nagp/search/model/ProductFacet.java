package com.nagarro.nagp.search.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFacet {
	
	private String facet_name;
	private List<String> facet_value;
}
