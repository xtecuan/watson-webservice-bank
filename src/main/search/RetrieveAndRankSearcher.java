package main.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

public class RetrieveAndRankSearcher extends Searcher {
	
	private boolean useReranking = false;
	
	public RetrieveAndRankSearcher(boolean useReranking) {
		this.useReranking = useReranking;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public List<ResultDocument> search(String query, int start, int num) {
		
		List<ResultDocument> search = new ArrayList<ResultDocument>();
		HttpSolrClient solrClient = RetrieveAndRankGlobal.getSolrClient();
		
		SolrQuery q = new SolrQuery(query);
		if (useReranking) {
			q.setParam("ranker_id", RetrieveAndRankGlobal.RANKER_ID);
			q.setRequestHandler("/fcselect");
		}
		q.setRows(num);
		q.setStart(start);
		q.setParam("fl", "id,title,url,body");
		
		QueryResponse response = null;
		try {
			response = solrClient.query(RetrieveAndRankGlobal.COLLECTION, q);
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		
		if (response == null)
			return search;
		
		this.setResponse(response.toString());
		this.setTotalelems(response.getResults().getNumFound());
		
		/* Parsear los resultados obtenidos */
		
		for (SolrDocument doc : response.getResults()) {
			int id = Integer.valueOf((String) doc.getFieldValue("id"));
			
			String title = "";
			for(String s : (ArrayList<String>) doc.getFieldValue("title")) {
				title += " " + s;
			}
			title = title.trim();
			
			String url = "";
			for(String s : (ArrayList<String>) doc.getFieldValue("url")) {
				url += " " + s;
			}
			url = url.trim();
			
			String text = "";
			for(String s : (ArrayList<String>) doc.getFieldValue("body")) {
				text += " " + s;
			}
			text = text.trim();
			
			search.add(new ResultDocument(id, title, url, text));
		}
		
		return search;
	}

}
