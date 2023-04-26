package com.nagarro.nagp.search.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dimension {

	private String unit;
	private double length;
	private double width;
	private double height;
}
