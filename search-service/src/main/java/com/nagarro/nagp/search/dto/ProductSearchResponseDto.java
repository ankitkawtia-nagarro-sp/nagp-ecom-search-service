package com.nagarro.nagp.search.dto;

import java.io.Serializable;
import java.util.List;

import com.nagarro.nagp.search.model.Product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class ProductSearchResponseDto implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8196952914175080250L;
	
	private List<Product> products; 
	private List<FacetDto> facetDtos;
}
