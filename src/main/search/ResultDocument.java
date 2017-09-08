package main.search;

public class ResultDocument {
	
	private int id;
	private String title;
	private String url;
	private String content;
	
	public ResultDocument(String title, String url, String content) {
		this(0, title, url, content);
	}
	public ResultDocument(Integer id, String title, String url, String content) {
		this.id = id;
		this.title = title;
		this.url = url;
		this.content = content;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	
}
