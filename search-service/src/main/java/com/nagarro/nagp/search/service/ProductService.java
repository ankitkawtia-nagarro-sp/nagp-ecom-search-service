package com.nagarro.nagp.search.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nagarro.nagp.search.constants.AppConstants;
import com.nagarro.nagp.search.dto.FacetDto;
import com.nagarro.nagp.search.dto.FacetValueDto;
import com.nagarro.nagp.search.dto.ProductSearchResponseDto;
import com.nagarro.nagp.search.dto.SearchQueryDto;
import com.nagarro.nagp.search.model.Product;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.util.BinaryData;
import co.elastic.clients.util.ContentType;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProductService {

	@Value("${product.index.data.loc}")
	public String productDataLoc;
	
	@Autowired
	ElasticsearchClient esClient;
	
	Aggregation aggs_brand,aggs_colors;
	
	public String createProductIndex(Product productDto) {
		
		String documentId = null;
		
		try {
			IndexResponse response = esClient.index(i->i
					.index("products")
					.id(productDto.getProduct_id())
					.document(productDto)
				);
			documentId = response.id();
			log.info("Indexed with version:"+response.version());
		} catch (ElasticsearchException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return documentId;
	}
	
	public List<String> indexBulkJson() throws Exception {
		log.info("Bulk indexing");
        File logDir = new File(productDataLoc);
        log.info("LogDir:" + logDir);
        //tag::bulk-json
        // List json log files in the log directory
        File[] files = logDir.listFiles();
        for(File file : files) {
        	log.info("Filename:" + file.getName());
        }
        File[] logFiles = logDir.listFiles(
            file -> file.getName().matches("product[0-9]*\\.json")
        );
        log.info("LogFiles:"+logFiles.length);
        List<String> documentIDs = new LinkedList<>();

        for (File file: logFiles) {
            FileInputStream input = new FileInputStream(file);
            BinaryData data = BinaryData.of(IOUtils.toByteArray(input), ContentType.APPLICATION_JSON);
            log.info("Data:"+data);
			
            IndexResponse response = esClient.index(i->i
					.index(AppConstants.PRODUCT_INDEX)
					.document(data)
				);
            documentIDs.add(response.id());
            log.info("Indexed {} with version:"+response.version(),response.id());
        }
        return documentIDs;
        //end::bulk-json
    }
	
	public ProductSearchResponseDto searchProducts(String searchText) {
		log.info("Search with query {}", searchText);
		List<Product> products = new LinkedList<>();
		ProductSearchResponseDto responseDto = new ProductSearchResponseDto();
		try {
			Query query = MatchQuery.of(m->m
					.field("name")
					.query(searchText)
					)._toQuery();
			
			
			SearchResponse<Product> response = esClient.search(s -> s
				    .index("products") 
				    .query(query)
				    .aggregations(AppConstants.PRODUCT_FACETS, a->a
				    		.nested(p->p.path(AppConstants.PRODUCT_FACETS))
				    		.aggregations("facet_names", 
				    				sa->sa
				    					.terms(f->f.field("product_facets.facet_name"))
				    					.aggregations("facet_values", ssa -> ssa
				    							.terms(f->f.field("product_facets.facet_value")))
				    				)
				    		),
				    Product.class      
				);
			List<FacetDto> facetDtos = new LinkedList<>();
			
			TotalHits total = response.hits().total();
			List<StringTermsBucket> facet_names = response.aggregations()
			.get(AppConstants.PRODUCT_FACETS)
			.nested()
			.aggregations()
			.get("facet_names")
			.sterms()
			.buckets().array();
			for(StringTermsBucket facet_name: facet_names) {
				FacetDto facetDto = new FacetDto();
				List<FacetValueDto> facetValuesDto = new LinkedList<>();
				
				List<StringTermsBucket> facet_values = facet_name.aggregations()
				.get("facet_values")
				.sterms()
				.buckets().array();
				for(StringTermsBucket facet_value : facet_values) {
					FacetValueDto facetValue = new FacetValueDto();
					facetValue.setFacetValueName(facet_value.key().stringValue());
					facetValue.setCount(facet_value.docCount());
					facetValuesDto.add(facetValue);
				}
				facetDto.setFacetName(facet_name.key().stringValue());
				facetDto.setFacetValueDto(facetValuesDto);
				facetDtos.add(facetDto);
			}
			
			boolean isExactResult = total.relation() == TotalHitsRelation.Eq;
			if(isExactResult) {
				log.info("There are "+ total.value()+ " results");
			}else {
				log.info("There are more than " + total.value() + " results");
			}
			List<Hit<Product>> hits = response.hits().hits();
			for(Hit<Product> hit : hits) {
				Product product = hit.source();
				products.add(product);
				log.info("Found product " + product.getProduct_id() + ", score "+ hit.score());
				log.info("Product variants:" + product.getVariants());
				product.getVariants().stream().forEach(v -> System.out.println(v));
			}
			responseDto.setProducts(products);
			responseDto.setFacetDtos(facetDtos);
		} catch (ElasticsearchException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseDto;
	}
	
	public ProductSearchResponseDto searchProductsWithFilters(SearchQueryDto searchQueryDto) {
		log.info("Search with query and filters{}", searchQueryDto.getTextQuery());
		List<Product> products = new LinkedList<>();
		ProductSearchResponseDto responseDto = new ProductSearchResponseDto();
		try {
			Query query = MatchQuery.of(m->m
					.field("name")
					.query(searchQueryDto.getTextQuery())
					)._toQuery();
			
			
			
			List<Query> listFilters = new LinkedList<>();
			for(SearchQueryDto.Filter filter:searchQueryDto.getFilters()) {
				
				List<Query> facetFilters = new LinkedList<>();
				Query facetNameTermFilter = new Query.Builder()
					    .term(t -> t                          
					        .field(AppConstants.PRODUCT_FACET_NAME_KEYWORD)                    
					        .value(filter.getKey())
					    )
					    .build();  
				List<FieldValue> fv = new LinkedList<>();
				filter.getValue().forEach(value -> fv.add(FieldValue.of(value)));
				Query facetValueTermFilter = new Query.Builder()
					    .terms(t -> t
					    		.field(AppConstants.PRODUCT_FACET_VALUE_KEYWORD)
					    		.terms(v-> v.value(fv))
					    )
					    .build();
				facetFilters.add(facetNameTermFilter);
				facetFilters.add(facetValueTermFilter);
				BoolQuery boolQuery = new BoolQuery.Builder()
						.filter(facetFilters)
						.build();
				Query query1 = new Query.Builder()
						.bool(boolQuery)
						.build();
				NestedQuery nestedQuery = new NestedQuery.Builder()
						.path(AppConstants.PRODUCT_FACETS)
						.query(query1)
						.build();
				System.out.println(nestedQuery.toString());
				listFilters.add(nestedQuery._toQuery());
			}
			
			SearchResponse<Product> response = esClient.search(s -> s
				    .index("products") 
				    .query(q->q.bool(
				    		b->b
				    		.must(query)
				    		.filter(listFilters)))
				    .aggregations(AppConstants.PRODUCT_FACETS, a->a
				    		.nested(p->p.path(AppConstants.PRODUCT_FACETS))
				    		.aggregations("facet_names", 
				    				sa->sa
				    					.terms(f->f.field("product_facets.facet_name"))
				    					.aggregations("facet_values", ssa -> ssa
				    							.terms(f->f.field("product_facets.facet_value")))
				    				)
				    		),
				    Product.class      
				);
			List<FacetDto> facetDtos = new LinkedList<>();
			
			TotalHits total = response.hits().total();
			List<StringTermsBucket> facet_names = response.aggregations()
			.get(AppConstants.PRODUCT_FACETS)
			.nested()
			.aggregations()
			.get("facet_names")
			.sterms()
			.buckets().array();
			for(StringTermsBucket facet_name: facet_names) {
				FacetDto facetDto = new FacetDto();
				List<FacetValueDto> facetValuesDto = new LinkedList<>();
				
				List<StringTermsBucket> facet_values = facet_name.aggregations()
				.get("facet_values")
				.sterms()
				.buckets().array();
				for(StringTermsBucket facet_value : facet_values) {
					FacetValueDto facetValue = new FacetValueDto();
					facetValue.setFacetValueName(facet_value.key().stringValue());
					facetValue.setCount(facet_value.docCount());
					facetValuesDto.add(facetValue);
				}
				facetDto.setFacetName(facet_name.key().stringValue());
				facetDto.setFacetValueDto(facetValuesDto);
				facetDtos.add(facetDto);
			}
			
			boolean isExactResult = total.relation() == TotalHitsRelation.Eq;
			if(isExactResult) {
				log.info("There are "+ total.value()+ " results");
			}else {
				log.info("There are more than " + total.value() + " results");
			}
			List<Hit<Product>> hits = response.hits().hits();
			for(Hit<Product> hit : hits) {
				Product product = hit.source();
				products.add(product);
				log.info("Found product " + product.getProduct_id() + ", score "+ hit.score());
				log.info("Product variants:" + product.getVariants());
				product.getVariants().stream().forEach(v -> System.out.println(v));
			}
			responseDto.setProducts(products);
			responseDto.setFacetDtos(facetDtos);
		} catch (ElasticsearchException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseDto;
	}
	
	public ProductSearchResponseDto searchProductsWithFacets(SearchQueryDto searchQueryDto) {
		log.info("Search with query and facets{}", searchQueryDto.getTextQuery());
		List<Product> products = new LinkedList<>();
		ProductSearchResponseDto responseDto = new ProductSearchResponseDto();
		try {
			Query query = MatchQuery.of(m->m
					.field("name")
					.query(searchQueryDto.getTextQuery())
					)._toQuery();
			
			List<Query> postFilters = new LinkedList<>();
			
			for(SearchQueryDto.Filter filter:searchQueryDto.getFilters()) {
				Query nestedQuery = createNestedQuery(filter.getKey(), filter.getValue());
				postFilters.add(nestedQuery);
				
				switch(filter.getKey().toLowerCase()) {
				case "colors":
					aggs_brand = createAggsFacet("brand",filter.getKey(),filter.getValue(),nestedQuery);
					break;
				case "brand":
					aggs_colors = createAggsFacet("colors",filter.getKey(),filter.getValue(),nestedQuery);
					break;
				}
			}
			Aggregation aggs_all_filters = createAggsAllFilters(postFilters);
			if(aggs_brand == null) {
				aggs_brand = createAggsFacet("brand",null,null,createNestedQuery(null, null));
			}
			if(aggs_colors == null) {
				aggs_colors = createAggsFacet("colors", null, null, createNestedQuery(null, null));
			}
			SearchResponse<Product> response = esClient.search(s -> s.index("products")
					.query(q -> q.bool(b -> b.must(query)))
					.aggregations(AppConstants.PRODUCT_AGGS_BRAND, aggs_brand)
					.aggregations(AppConstants.PRODUCT_AGGS_COLORS,aggs_colors)
					.aggregations(AppConstants.PRODUCT_AGGS_ALL_FILTERS, aggs_all_filters)
					.postFilter(pf-> pf.bool(b->b.filter(postFilters))),
					Product.class);
			List<FacetDto> facetDtos = new LinkedList<>();
			System.out.println("Response:"+ response);
			TotalHits total = response.hits().total();
			facetDtos.add(getFacetDto(response, AppConstants.PRODUCT_AGGS_BRAND));
			facetDtos.add(getFacetDto(response, AppConstants.PRODUCT_AGGS_COLORS));
			getFacetDtos(response,facetDtos);
			
			
			boolean isExactResult = total.relation() == TotalHitsRelation.Eq;
			if(isExactResult) {
				log.info("There are "+ total.value()+ " results");
			}else {
				log.info("There are more than " + total.value() + " results");
			}
			List<Hit<Product>> hits = response.hits().hits();
			for(Hit<Product> hit : hits) {
				Product product = hit.source();
				products.add(product);
				log.info("Found product " + product.getProduct_id() + ", score "+ hit.score());
				log.info("Product variants:" + product.getVariants());
				product.getVariants().stream().forEach(v -> System.out.println(v));
			}
			responseDto.setProducts(products);
			responseDto.setFacetDtos(facetDtos);
			aggs_brand = null;
			aggs_colors = null;
			
		} catch (ElasticsearchException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseDto;
	}

	private void getFacetDtos(SearchResponse<Product> response, List<FacetDto> facetDtos) {
		// TODO Auto-generated method stub
		List<StringTermsBucket> facet_names = response.aggregations()
				.get(AppConstants.PRODUCT_AGGS_ALL_FILTERS)
				.filter()
				.aggregations()
				.get(AppConstants.PRODUCT_FACETS)
				.nested()
				.aggregations()
				.get("facet_names")
				.sterms()
				.buckets().array();
				for(StringTermsBucket facet_name: facet_names) {
					
					String facetName = facet_name.key().stringValue();
					if(facetName.equals("brand") || facetName.equals("colors"))
						continue;
					
					FacetDto facetDto = new FacetDto();
					List<FacetValueDto> facetValuesDto = new LinkedList<>();
					
					List<StringTermsBucket> facet_values = facet_name.aggregations()
					.get("facet_values")
					.sterms()
					.buckets().array();
					for(StringTermsBucket facet_value : facet_values) {
						FacetValueDto facetValue = new FacetValueDto();
						facetValue.setFacetValueName(facet_value.key().stringValue());
						facetValue.setCount(facet_value.docCount());
						facetValuesDto.add(facetValue);
					}
					facetDto.setFacetName(facetName);
					facetDto.setFacetValueDto(facetValuesDto);
					facetDtos.add(facetDto);
				}
		
	}

	private Aggregation createAggsAllFilters(List<Query> postFilters) {
		// TODO Auto-generated method stub
		Aggregation aggsAllFilters = Aggregation.of(a->a.filter(f->f.bool(b->b.filter(postFilters)))
														.aggregations(AppConstants.PRODUCT_FACETS, sa -> sa
																.nested(p->p.path(AppConstants.PRODUCT_FACETS))
																.aggregations("facet_names", ssa->ssa
																		.terms(f->f.field(AppConstants.PRODUCT_FACET_NAME_KEYWORD))
																		.aggregations("facet_values", sssa->sssa
																				.terms(f->f.field(AppConstants.PRODUCT_FACET_VALUE_KEYWORD))))));
		System.out.println("AggsAllFilters::"+aggsAllFilters);
		return aggsAllFilters;
	}
	
	private Aggregation createAggsFacet(String facetName,String key, List<String> values,Query nestedQuery) {
		// TODO Auto-generated method stub
		Aggregation aggsFacet = null;
		List<Query> listFilters = new LinkedList<>();
		
		listFilters.add(nestedQuery);
		BoolQuery outerBoolQuery = new BoolQuery.Builder()
				.filter(listFilters)
				.build();
		System.out.println("OuterBoolQuery:"+outerBoolQuery);
		
		MatchQuery brandMatchQuery = MatchQuery.of(m->m.field(AppConstants.PRODUCT_FACET_NAME_KEYWORD)
				.query(facetName));
		System.out.println("Match Query:"+ brandMatchQuery);
		aggsFacet = Aggregation.of(a->a.filter(f->f.bool(outerBoolQuery)).aggregations(AppConstants.PRODUCT_FACETS, sa -> sa
				.nested(p -> p.path(AppConstants.PRODUCT_FACETS))
				.aggregations("aggs_special", ssa -> ssa.filter(f -> f.match(brandMatchQuery)).aggregations(
						"facet_names",
						sssa -> sssa.terms(f -> f.field(AppConstants.PRODUCT_FACET_NAME_KEYWORD))
								.aggregations("facet_values", ssssa -> ssssa
										.terms(f -> f.field(AppConstants.PRODUCT_FACET_VALUE_KEYWORD))))))
					);
		System.out.println("Aggs"+facetName+"::"+aggsFacet);
		return aggsFacet;
		
	}
	
	private Query createNestedQuery(String key, List<String> values) {
		List<Query> facetFilters = new LinkedList<>();
		
		if(key != null && values != null) {
		Query facetNameTermFilter = new Query.Builder()
			    .term(t -> t                          
			        .field(AppConstants.PRODUCT_FACET_NAME_KEYWORD)                    
			        .value(key)
			    )
			    .build();  
		List<FieldValue> fv = new LinkedList<>();
		values.forEach(value -> fv.add(FieldValue.of(value)));
		Query facetValueTermFilter = new Query.Builder()
			    .terms(t -> t
			    		.field(AppConstants.PRODUCT_FACET_VALUE_KEYWORD)
			    		.terms(v-> v.value(fv))
			    )
			    .build();
		facetFilters.add(facetNameTermFilter);
		facetFilters.add(facetValueTermFilter);
		}
		BoolQuery boolQuery = new BoolQuery.Builder()
				.filter(facetFilters)
				.build();
		Query query1 = new Query.Builder()
				.bool(boolQuery)
				.build();
		NestedQuery nestedQuery = new NestedQuery.Builder()
				.path(AppConstants.PRODUCT_FACETS)
				.query(query1)
				.build();
		System.out.println(nestedQuery.toString());
		return nestedQuery._toQuery();
	}
	
	private FacetDto getFacetDto(SearchResponse<Product> response,String aggsName) {
		List<StringTermsBucket> facet_names = response.aggregations().get(aggsName).filter()
				.aggregations().get(AppConstants.PRODUCT_FACETS).nested().aggregations().get("aggs_special").filter()
				.aggregations().get("facet_names").sterms().buckets().array();
		FacetDto facetDto = new FacetDto();
		for (StringTermsBucket facet_name : facet_names) {

			List<FacetValueDto> facetValuesDto = new LinkedList<>();

			List<StringTermsBucket> facet_values = facet_name.aggregations().get("facet_values").sterms().buckets()
					.array();
			for (StringTermsBucket facet_value : facet_values) {
				FacetValueDto facetValue = new FacetValueDto();
				facetValue.setFacetValueName(facet_value.key().stringValue());
				facetValue.setCount(facet_value.docCount());
				facetValuesDto.add(facetValue);
			}
			facetDto.setFacetName(facet_name.key().stringValue());
			facetDto.setFacetValueDto(facetValuesDto);

		}
		return facetDto;
	}
}
