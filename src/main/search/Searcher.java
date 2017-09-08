package main.search;

import java.util.List;

public abstract class Searcher {
	
	private long totalelems;
	private String response;
	
	public long getTotalelems() {
		return totalelems;
	}

	public void setTotalelems(long totalelems) {
		this.totalelems = totalelems;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public abstract List<ResultDocument> search (String query, int start, int num);
}
