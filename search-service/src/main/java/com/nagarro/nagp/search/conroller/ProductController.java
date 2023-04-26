package com.nagarro.nagp.search.conroller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nagarro.nagp.search.dto.ProductSearchResponseDto;
import com.nagarro.nagp.search.dto.SearchQueryDto;
import com.nagarro.nagp.search.model.Product;
import com.nagarro.nagp.search.service.ProductService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/")
@Slf4j
public class ProductController {

	@Autowired
	private ProductService productService;
	
	@GetMapping("/")
	public ResponseEntity<String> heatlhStatus(){
		return ResponseEntity.ok("Health is OK");
	}
	
	@PostMapping("/products/create")
	public ResponseEntity<String> createProductIndex(@RequestBody Product productDto){
		
		String docID = productService.createProductIndex(productDto);
		return new ResponseEntity<String>(docID,HttpStatus.OK);
	}
	
	@GetMapping("/products/bulk")
	public ResponseEntity<List<String>> createProductIndexBulk(){
		log.info("bulk index");
		List<String> docIDs = null;
		try {
			docIDs = productService.indexBulkJson();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResponseEntity<List<String>>(docIDs, HttpStatus.OK);
	}
	
	@GetMapping("/search")
	public ResponseEntity<ProductSearchResponseDto> searchText(@RequestParam(required = false) String query){
		
		log.info("searching by name {}",query);
		ProductSearchResponseDto products = productService.searchProducts(query);
		return new ResponseEntity<ProductSearchResponseDto>(products, HttpStatus.OK);
	}
	
	@PostMapping("/search")
	public ResponseEntity<ProductSearchResponseDto> search(@RequestBody SearchQueryDto searchQueryDto){
		
		log.info("searching by searchquery and filter {}");
		ProductSearchResponseDto products = productService.searchProductsWithFacets(searchQueryDto);
		return new ResponseEntity<ProductSearchResponseDto>(products, HttpStatus.OK);
	}
	
	
}
