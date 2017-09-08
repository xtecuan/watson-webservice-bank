package main.WEXindexexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WEXDumpToJSON {

	public static void main(String[] args) {
		
		NodeList list = null;
		PrintWriter pw = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			Document document = builder.parse(new File(WEXDump.FILENAME));
			document.getDocumentElement().normalize();
			
			list = document.getElementsByTagName("document");

			pw = new PrintWriter(new FileWriter(new File("C:/Users/trsomp/Documents/Watson/Retrieve&Rank/TestCollection.json")));
//			pw = new PrintWriter(new FileWriter(new File("C:/Users/trsomp/Documents/Watson/Retrieve&Rank/TestCollection_reduced.json")));
		} catch (ParserConfigurationException | SAXException | IOException e) {
		}
		
		/* Crear el JSON con los elementos devueltos (y con su resumen generado) */
		
		pw.println("{");
		
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
			
//			if (text.isEmpty())
//				continue;
			
			/* Formar el JSON */
			
			pw.println("\t\"add\" : {");
			pw.println("\t\t\"doc\" : {");
			pw.println("\t\t\t\"id\" : " + (i+1) + ",");
			pw.println("\t\t\t\"title\" : \"" + title + "\",");
			pw.println("\t\t\t\"url\" : \"" + url + "\",");
			pw.println("\t\t\t\"body\" : \"" + text + "\"");
			pw.println("\t\t}");
			pw.println("\t},");

		}

		pw.println("\t\"commit\" : { }");
		
		pw.println("}");
		
		pw.close();
		
	}

}
