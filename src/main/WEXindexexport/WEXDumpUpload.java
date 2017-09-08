package main.WEXindexexport;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import main.search.RetrieveAndRankGlobal;

public class WEXDumpUpload {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		
		String collection = "last_collection";
		String config = "spanish_config";
		HttpSolrClient solrClient = RetrieveAndRankGlobal.getSolrClient();
		
		/* Crear colleccion de prueba */
		
		CollectionAdminRequest.Create createCollectionRequest = new CollectionAdminRequest.Create();
		createCollectionRequest.setCollectionName(collection);
		createCollectionRequest.setConfigName(config);

		System.out.println("Creating collection...");
		CollectionAdminResponse response;
		try {
			response = createCollectionRequest.process(solrClient);
		    if (!response.isSuccess()) {
		      System.out.println(response.getErrorMessages());
		      throw new IllegalStateException("Failed to create collection: "
		          + response.getErrorMessages().toString());
		    }

			System.out.println("Collection created.");
			System.out.println(response);
			
		} catch (SolrServerException | IOException e1) {
			e1.printStackTrace();
			System.err.println("Error creando la colección");
			return;
		}
		
		/* Indexación de documentos */ 
		
		NodeList list = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			Document document = builder.parse(new File(WEXDump.FILENAME));
			document.getDocumentElement().normalize();
			
			list = document.getElementsByTagName("document");
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
		}
		
		/* Subir al índice de Solr los elementos devueltos (y con su resumen generado) */
		
		for (int i = 0; i < list.getLength(); i++) {
			Element element = (Element) list.item(i);
			
			String title = "";
			String text = "";
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
				text = text.replace("\n", " ").replace("\r", " ");				
				text = text.trim();
			}
			
			String url = element.getAttribute("url");
			
			if (text.isEmpty() || title.isEmpty() || url == null || url.isEmpty())
				continue;
			
			/* Añadir el documento para indexar */
			
			SolrInputDocument doc = new SolrInputDocument();
			doc.addField("id", (i+1));
			doc.addField("title", title);
			doc.addField("url", url);
			doc.addField("body", text);
			
			boolean error = false;
			do {
				try {
					UpdateResponse addResponse = solrClient.add(collection, doc);
					System.out.println(doc.getFieldValue("id") + ": " + addResponse);
					error = false;
				} catch (Exception e) {
					System.out.println(doc.getFieldValue("id") + " (" + doc.getFieldValue("url") + ") ERROR");
					e.printStackTrace();
					error = true;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			} while(error);
		}
		
		try {
			solrClient.commit(collection);
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		
		try {
			solrClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
