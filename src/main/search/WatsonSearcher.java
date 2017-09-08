package main.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class WatsonSearcher extends Searcher {

	@Override
	public List<ResultDocument> search(String query, int start, int num) {
		
		List<ResultDocument> search = new ArrayList<ResultDocument>();
		
		/* Hacer llamada para obtener los resultados de la busqueda */
		
		Client client = Client.create(new DefaultClientConfig());
		WebResource service = client.resource("http://10.192.0.9/vivisimo/cgi-bin/velocity.exe");
		WebResource msgService = service
				.queryParam("v.function", "query-search")
				.queryParam("v.indent", "true")
				.queryParam("query", query)
				.queryParam("sources", "PublicationsIADB")
				.queryParam("v.username", "api-user")
				.queryParam("v.password", "TH1nk1710")
				.queryParam("v.app", "api-rest")
				.queryParam("num", num + "")
				.queryParam("start", start + "")
				.queryParam("output-contents-mode", "list")
				.queryParam("output-contents", "title%20snippet");
		
		String response = msgService.accept(MediaType.APPLICATION_XML).get(String.class);
		this.setResponse(response);
		
		System.err.println("****************************************************");
		System.err.println(response);
		
		/* Parsear la respuesta obtenida */
		
		NodeList list = null;
		Element source = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(response));
			
			Document document = builder.parse(is);
			document.getDocumentElement().normalize();
			
			list = document.getElementsByTagName("document");
			source = (Element) document.getElementsByTagName("added-source").item(0);
			System.err.println("****************************************************");
			System.err.println(source);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		
		if (list == null  || source == null) {
			return search;
		}
		
		this.setTotalelems(Long.valueOf(source.getAttribute("total-results")));
		
		/* Devolver la lista de documentos */
		
		for (int i = 0; i < list.getLength(); i++) {
			Element element = (Element) list.item(i);

			String title = "";
			String text = "";
			String url = element.getAttribute("url");
			
			if (element.getElementsByTagName("content").getLength() > 0) {
				
				// title
				NodeList titleNodes = element.getElementsByTagName("content").item(0).getChildNodes();
				for (int j = 0; j < titleNodes.getLength(); j++) {
					title += " " + titleNodes.item(j).getNodeValue().replace("\t", " ");
				}
				title = title.trim();
				
				// text
				NodeList textNodes = element.getElementsByTagName("content").item(1).getChildNodes();
				for (int j = 0; j < textNodes.getLength(); j++) {
					text += " " + textNodes.item(j).getNodeValue().replace("\t", " ");
				}
				text = text.trim();
			}
			
			search.add(new ResultDocument(title, url, text));
		}
		
		return search;
	}

}
