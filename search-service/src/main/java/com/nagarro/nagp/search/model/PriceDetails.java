package com.nagarro.nagp.search.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDetails {

	private String currency;
	private double mrp;
	private double sale_price;
	private double discount;
}
