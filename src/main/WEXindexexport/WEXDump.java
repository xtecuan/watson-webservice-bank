package main.WEXindexexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class WEXDump {
	
	public static String FILENAME = "C:/Users/trsomp/Documents/Watson/Retrieve&Rank/dumpTestCollection.xml";

	public static void main(String[] args) {
		
		/* Hacer llamada para obtener los resultados de la busqueda vacía (todos los elementos) */
		
		Client client = Client.create(new DefaultClientConfig());
		WebResource service = client.resource("http://213.27.133.110/vivisimo/cgi-bin/velocity.exe");
		WebResource msgService = service
				.queryParam("v.function", "query-search")
				.queryParam("v.indent", "true")
				.queryParam("query", "") // query vacía para que devuelva todos los resultados
				.queryParam("sources", "PublicationsIADB")
				.queryParam("v.username", "api-user")
				.queryParam("v.password", "TH1nk1710")
				.queryParam("v.app", "api-rest")
				.queryParam("num", "99999999") // numero suficientemente grande para que devuelva todos los documentos
				.queryParam("start", "0")
				.queryParam("output-contents-mode", "list")
				.queryParam("output-contents", "title%20snippet");
		
		/* Guardar en fichero la respuesta obtenida */

		String response = msgService.accept(MediaType.APPLICATION_XML).get(String.class);
		
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(new File(FILENAME)));
			pw.println(response);			
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
