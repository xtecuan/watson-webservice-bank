package main.search;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import com.ibm.watson.developer_cloud.retrieve_and_rank.v1.RetrieveAndRank;
import com.sun.deploy.net.proxy.*;

public class RetrieveAndRankGlobal {

	public static final String USERNAME 		= "45fe09b9-75de-4236-ab42-60329199fdf0";
	public static final String PASSWORD 		= "SkrzGHYnLSPU";
	public static final String SOLR_CLUSTER_ID	= "sce5fa22f7_5b67_4c54_bc2a_d9cf2fcfaa65";
	public static final String RANKER_ID 		= "1eec74x28-rank-4213";
	public static final String COLLECTION 		= "idb_collection";

	private static HttpSolrClient solrClient = null;
	private static RetrieveAndRank service = null;
	
	public static HttpSolrClient getSolrClient() {
		if (solrClient == null) {
			solrClient = getSolrClient(getService().getSolrUrl(SOLR_CLUSTER_ID), USERNAME, PASSWORD);
		}
		return solrClient;
	}
	
	private static RetrieveAndRank getService() {
		if (service == null) {
			service = new RetrieveAndRank();
			service.setUsernameAndPassword(RetrieveAndRankGlobal.USERNAME, RetrieveAndRankGlobal.PASSWORD);
		}
		return service;
	}

	@SuppressWarnings("deprecation")
	private static HttpSolrClient getSolrClient(String uri, String username, String password) {
	    return new HttpSolrClient(service.getSolrUrl(SOLR_CLUSTER_ID), createHttpClient(uri, username, password));
	}
	
	
	private static HttpClient createHttpClient(String uri, String username, String password) {
		
	    final URI scopeUri = URI.create(uri);

	    final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();


//	    String proxy = getProxyConf(scopeUri);
	    	    
//	    CredentialsProvider credsProvider = new BasicCredentialsProvider();

	    AuthScope siteScope = new AuthScope(scopeUri.getHost(), scopeUri.getPort());
	    Credentials siteCreds = new UsernamePasswordCredentials(USERNAME, PASSWORD);
	    credsProvider.setCredentials(siteScope, siteCreds);
	    
	    AuthScope proxyScope = new AuthScope("wwppool.local.iadb.org", 9090);
	    Credentials proxyCreds = new UsernamePasswordCredentials("pl-idb", "Summer2017");
	    credsProvider.setCredentials(proxyScope, proxyCreds);
	    
	    final HttpClientBuilder builder = HttpClientBuilder.create()
	        .setMaxConnTotal(128)
	        .setMaxConnPerRoute(32)
	        //.setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT).setRedirectsEnabled(true).build())
	        .setDefaultCredentialsProvider(credsProvider)
	        .setProxy(new HttpHost("wwppool.local.iadb.org",9090,"http"))
	        ;
	    
//	    if (proxy.contains("PROXY")) {
//	    	System.out.println("NO ES DIRECT");
//	    	int port = Integer.parseInt(proxy.split(" ")[1].split(":")[1]);
//	    	String hostname = proxy.split(" ")[1].split(":")[0];
//	    	
//	    	builder.setProxy(new HttpHost(hostname,port));
//	    }
	    
	    return builder.build();
	}
	
	
}
