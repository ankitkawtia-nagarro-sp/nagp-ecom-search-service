package com.nagarro.nagp.search.config;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class ElasticSearchClientConfig {

	@Value("${elasticsearch.hostname}")
    private String elasticSearchHost;
	
    @Value("${elasticsearch.port}")
    private int elasticSearchPort;
    
    @Value("${elasticsearch.username}")
    private String elasticSearchUser;
    
    @Value("${elasticsearch.password}")
    private String elasticSearchPass;
    
    @Value("${elasticsearch.apikey}")
    private String apiKey;
    
    @Value("${elasticsearch.security.enabled}")
    private boolean securityEnabled;
    
    @Value("${elasticsearch.security.fingerprint}")
    private String fingerprint;
    
	  @Bean 
	  public ElasticsearchClient elasticsearchClient() throws Exception { //tag::create-client
	  //Create the low-level client 
	   if(securityEnabled)
		   //return createSecureClientFingerPrint();
		   return createSecureClient();
	   else 
		   return createClient();
	  
	  }
	  
	  public ElasticsearchClient createSecureClientFingerPrint() throws Exception {

	        // Create the low-level client
	        String host = elasticSearchHost;
	        int port = elasticSearchPort;
	        String login = elasticSearchUser;
	        String password = elasticSearchPass;

	        //tag::create-secure-client-fingerprint

	        SSLContext sslContext = TransportUtils
	            .sslContextFromCaFingerprint(fingerprint); // <1>

	        BasicCredentialsProvider credsProv = new BasicCredentialsProvider(); // <2>
	        credsProv.setCredentials(
	            AuthScope.ANY, new UsernamePasswordCredentials(login, password)
	        );

	        RestClient restClient = RestClient
	            .builder(new HttpHost(host, port, "https")) // <3>
	            .setHttpClientConfigCallback(hc -> hc
	                .setSSLContext(sslContext) // <4>
	                .setDefaultCredentialsProvider(credsProv)
	            )
	            .build();

	        // Create the transport and the API client
	        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
	        ElasticsearchClient client = new ElasticsearchClient(transport);
	        //end::create-secure-client-fingerprint
	        return client;
	    }
	  
	  public ElasticsearchClient createSecureClient() throws Exception {

	        // Create the low-level client
	        String host = elasticSearchHost;
	        int port = elasticSearchPort;
	        String login = elasticSearchUser;
	        String password = elasticSearchPass;

	        BasicCredentialsProvider credsProv = new BasicCredentialsProvider(); // <2>
	        credsProv.setCredentials(
	            AuthScope.ANY, new UsernamePasswordCredentials(login, password)
	        );

	        RestClient restClient = RestClient
	            .builder(new HttpHost(host, port, "https")) // <3>
	            .setHttpClientConfigCallback(hc -> hc
	                .setDefaultCredentialsProvider(credsProv)
	            )
	            .build();

	        // Create the transport and the API client
	        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
	        ElasticsearchClient client = new ElasticsearchClient(transport);
	        //end::create-secure-client-fingerprint
	        return client;
	    }
	  
	  public ElasticsearchClient createClient() throws Exception {
	        //tag::create-client
	        // Create the low-level client
	        RestClient restClient = RestClient.builder(
	            new HttpHost("localhost", 9200)).build();

	        // Create the transport with a Jackson mapper
	        ElasticsearchTransport transport = new RestClientTransport(
	            restClient, new JacksonJsonpMapper());

	        // And create the API client
	        ElasticsearchClient client = new ElasticsearchClient(transport);
	        //end::create-client
	        return client;
	  }

	 
}
