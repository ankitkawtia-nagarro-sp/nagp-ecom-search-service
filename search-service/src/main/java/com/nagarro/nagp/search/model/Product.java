package com.nagarro.nagp.search.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	private String product_id;
	private String name;
	private String description;
	private String category;
	private String brand;
	private String status;
	private List<Img> imgs;
	private Dimension dimensions;
	private String weight;
	private int qty;
	private List<Attribute> attrs;
	private MerchantInfo merchant_info;
	private PriceDetails price_details;
	private List<ProductFacet> product_facets;
	private List<Object> variants;
}
