package com.nagarro.nagp.search.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfo {
	
	private String id;
	private String name;
	private String address;
	private String city;
	private String state;
	private String zipcode;
	private String country;
}
