package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.DefaultValue;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import main.java.ie.dcu.cngl.summarizer.Aggregator;
import main.java.ie.dcu.cngl.summarizer.Summarizer;
import main.java.ie.dcu.cngl.summarizer.Weighter;
import main.java.ie.dcu.cngl.tokenizer.Structurer;
import main.java.ie.dcu.cngl.tokenizer.TokenInfo;
import main.search.ResultDocument;
import main.search.RetrieveAndRankSearcher;
import main.search.Searcher;
import main.search.WatsonSearcher;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

@Path("api")
public class Services {

    private static final int USE_WATSON = 0;
//	private static final int USE_RAR_ORIGINAL = 1;
    private static final int USE_RAR_RERANKED = 2;

    private static final Logger logger = org.apache.log4j.Logger.getLogger(Services.class);

    /**
     * Método REST que realiza un resumen estático del contenido pasado por
     * argumento.
     *
     * @param sentnum Número de sentencias en las que queremos resumir el
     * contenido.
     * @param text Texto completo a resumir.
     * @return XML con el resumen realizado (incluyendo también los argumentos
     * de entrada).
     */
    @GET
    public Response summaryREST(
            @QueryParam(value = "sentnum") int sentnum,
            @QueryParam(value = "text") String text) {

        if (text == null || text.isEmpty() || sentnum <= 0) {
            System.err.println("Argumentos de entrada inválidos:");
            System.err.println("\ttext = '" + text + "'");
            System.err.println("\tsentnum = " + sentnum);
            return Response.status(500).entity(null).build();
        }

        Structurer structurer = new Structurer();
        Weighter weighter = new Weighter();
        Aggregator aggregator = new Aggregator();

        Summarizer summarizer = new Summarizer(structurer, weighter, aggregator);
        summarizer.setNumSentences(sentnum);
        String summary = summarizer.summarize(text);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sentnum", sentnum);
        jsonObject.put("text", text);
        jsonObject.put("result", summary);

        return Response.status(200).entity(XML.toString(jsonObject)).build();
    }

    /**
     * Método REST que devuelve los documentos con sus resúmenes dinámicos
     * respecto a una búsqueda realizada.
     *
     * A partir de una búsqueda realizada por el usuario, se llama a este método
     * REST. Éste utiliza la API de Watson para realizar la búsqueda de la query
     * en la colección elegida. Cada documento devuelto contiene la URL, el
     * título y su contenido. Por cada uno de los documentos, se realiza el
     * resumen dinámico de su contenido según la query pedida. Se devuelve los
     * resultados de la búsqueda con el resumen realizado.
     *
     * @param startpage Página a devolver de los resultados (comienza por la 1)
     * @param sentnum Número de sentencias en las que queremos resumir el
     * contenido de cada documento resultado.
     * @param query Búsqueda realizada por el usuario.
     * @param use Identificador del searcher a utilizar (0=Watson, 1=R&R,
     * 2=Reranked R&R)
     * @return JSON con el ranking de documentos para la query realizada. Cada
     * documento viene definido por su URL, su título y su resumen dinámico.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchREST(
            @FormParam(value = "start") int startpage,
            @FormParam(value = "sentnum") int sentnum,
            @FormParam(value = "query") String query,
            @FormParam(value = "use") int use) {

        /* Validar argumentos de entrada */
        int start = (startpage - 1) * 10;
        if (start < 0) {
            start = 0;
        }

        if (sentnum <= 0) {
            sentnum = 2;
        }

        if (query == null) {
            return Response.status(500).entity("Parameter 'query' is required.").build();
        }
        String encodedQuery = query;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        /* Crear JSONs a devolver */
        JSONObject result = new JSONObject();
        JSONArray elements = new JSONArray();

        /* Realizar la búsqueda */
        Searcher searcher = use == USE_WATSON ? new WatsonSearcher() : new RetrieveAndRankSearcher(use == USE_RAR_RERANKED);
        List<ResultDocument> search = searcher.search(encodedQuery, start, 10);

        String docs = "";
        int pos = start;
        for (ResultDocument doc : search) {

            /* Hacer el resumen del contenido del documento */
            Structurer structurer = new Structurer();
            Weighter weighter = new Weighter();
            Aggregator aggregator = new Aggregator();
            Summarizer summarizer = new Summarizer(structurer, weighter, aggregator);
            summarizer.setNumSentences(sentnum);
            summarizer.setQuery(query);
            String summary = null;
            try {
                summary = summarizer.summarize(doc.getContent());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(doc.getContent());
            }

            /* Añadir el elemento al JSON a devolver */
            JSONObject jsonelement = new JSONObject();
            jsonelement.put("id", doc.getId());
            jsonelement.put("title", doc.getTitle());
            jsonelement.put("url", doc.getUrl());
            jsonelement.put("summary", summary);

            elements.put(jsonelement);

            docs += "(" + doc.getId() + "," + (++pos) + ") ";
        }

        /* Crear el JSON con la información para la paginación */
        int realstart = start + 1;

        int retrieved = 0;
        try {
            retrieved = Integer.valueOf(search.size());
        } catch (Exception e) {
            retrieved = 0;
        }

        long total = searcher.getTotalelems();

        /* Crear el JSON a devolver */
        JSONObject info = new JSONObject();
        info.put("page", (realstart / 10 + 1) + "");
        info.put("from", realstart + "");
        info.put("to", (realstart + retrieved - 1) + "");
        info.put("total", total + "");
        info.put("totalpages", ((total - 1) / 10 + 1) + "");

        result.put("info", info);
        result.put("elements", elements);

        /* DEBUG: Para analizar la respuesta obtenida de Watson */
//		try {
//			PrintWriter pw = new PrintWriter(new FileWriter(new File("C:/xampp/tomcat/webapps/response_" + realstart + "-" + (realstart+retrieved-1) + ".xml")));
//			pw.println(searcher.getResponse());
//			pw.close();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
        /* Guardar la interacción con el usuario */
        if (use != USE_WATSON) {
            ArrayList<String> cmdList = new ArrayList<String>();
            cmdList.add("cmd");
            cmdList.add("/c");
            cmdList.add("C:\\Python27\\python.exe");
            cmdList.add("C:\\GMV\\KnowledgeApp\\Documents\\busqueda.py");
            cmdList.add("\"" + query + "\"");
            cmdList.addAll(Arrays.asList(docs.split(" ")));

            try {
                ProcessBuilder pb = new ProcessBuilder(cmdList);
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                InputStream stdin = proc.getInputStream();
                InputStreamReader isr = new InputStreamReader(stdin);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                System.out.println("<OUTPUT>");
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("</OUTPUT>");
                proc.destroy();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Response.status(200).entity(result.toString()).build();
    }

    /**
     * Método REST que llama a un script en python para almacenar el click del
     * usuario sobre el documento seleccionado.
     *
     * @param query Búsqueda realizada por el usuario
     * @param doc Documento que el usuario ha hecho click
     * @return 200 OK
     */
    @POST
    @Path("/click")
    public Response actionClick(
            @FormParam(value = "query") String query,
            @FormParam(value = "doc") int doc) {

        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add("cmd");
        cmdList.add("/c");
        cmdList.add("C:\\Python27\\python.exe");
        cmdList.add("C:\\GMV\\KnowledgeApp\\Documents\\click.py");
        cmdList.add("\"" + query + "\"");
        cmdList.add(doc + "");

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            InputStream stdin = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            System.out.println("<OUTPUT>");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("</OUTPUT>");
            proc.destroy();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Response.status(200).build();
    }

    // Changes done by xtecuan at gmail dot com
    private Response wrongOrMissingParameterError(String paramName, String paramValue) {
        String message = "";
        if (paramValue == null || paramValue.equals("")) {
            message = "Parameter " + paramName + " : is required !";
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(message).type("text/plain").build();
    }

    private boolean validateParam(String paramValue) {
        return paramValue != null && !paramValue.equals("") && paramValue.length() > 0;
    }

    private String encodeQuery(String query) {
        String encodedQuery = query;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        return encodedQuery;
    }

    private JSONObject makeSummary(int sentnum, String query, ResultDocument doc) {
        JSONObject jsonelement = new JSONObject();
        Structurer structurer = new Structurer();
        Weighter weighter = new Weighter();
        List<TokenInfo> tokens = new ArrayList<>();
        tokens.add(new TokenInfo(query));
        weighter.setQuery((ArrayList<TokenInfo>) tokens);
        Aggregator aggregator = new Aggregator();
        Summarizer summarizer = new Summarizer(structurer, weighter, aggregator);
        summarizer.setNumSentences(sentnum);
        summarizer.setQuery(query);
        String summary = null;
        try {
            summary = summarizer.summarize(doc.getContent());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(doc.getContent());
        }
        jsonelement.put("id", doc.getId());
        jsonelement.put("title", doc.getTitle());
        jsonelement.put("url", doc.getUrl());
        jsonelement.put("summary", summary);
        jsonelement.put("searchTerm", query);
        return jsonelement;
    }

    private JSONArray getThreeResumesFromDocument(String[] keys, Searcher searcher, int start, int sentnum) {
        JSONArray elements = new JSONArray();
        List<ResultDocument> search = searcher.search(encodeQuery(keys[0]), 1, 1);
        String docs = "";
        int pos = start;
        for (ResultDocument doc : search) {
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                JSONObject jsonelement = makeSummary(sentnum, key, doc);
                elements.put(jsonelement);
            }
            /* Añadir el elemento al JSON a devolver */
            docs += "(" + doc.getId() + "," + (++pos) + ") ";
        }

        return elements;
    }

    @GET
    @Path("/searchOneDocSample")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchNRest(
            @QueryParam(value = "start") @DefaultValue("1") int startpage,
            @QueryParam(value = "sentnum") @DefaultValue("3") int sentnum,
            @QueryParam(value = "query") String query,
            @QueryParam(value = "use") @DefaultValue("0") int use) {

        if (validateParam(query)) {
            int start = (startpage - 1) * 10;
            if (start < 0) {
                start = 0;
            }

            if (sentnum <= 0) {
                sentnum = 2;
            }

            String[] queries = query.split(",");

            if (queries.length > 3) {
                return Response.status(Response.Status.BAD_REQUEST).entity("The app allows a maximum of three queries at once").type("text/plain").build();
            } else {
                JSONObject result = new JSONObject();
                Searcher searcher = use == USE_WATSON ? new WatsonSearcher() : new RetrieveAndRankSearcher(use == USE_RAR_RERANKED);
                JSONArray elements = getThreeResumesFromDocument(queries, searcher, startpage, sentnum);
                /* Crear el JSON con la información para la paginación */
                int realstart = start + 1;

                int retrieved = 0;
                try {
                    retrieved = Integer.valueOf(1);
                } catch (Exception e) {
                    retrieved = 0;
                }

                long total = searcher.getTotalelems();

                /* Crear el JSON a devolver */
                JSONObject info = new JSONObject();
                info.put("page", (realstart / 10 + 1) + "");
                info.put("from", realstart + "");
                info.put("to", (realstart + retrieved - 1) + "");
                info.put("total", total + "");
                info.put("totalpages", ((total - 1) / 10 + 1) + "");

                result.put("info", info);
                result.put("elements", elements);

                return Response.status(200).entity(result.toString()).build();

            }

        } else {
            return wrongOrMissingParameterError("query", query);
        }

    }
    //Hasta aca no funciono lo de los resumenes con la libreria primera prueba

    public static final String BASE_PATH = "C:\\Work\\KNM_Projects\\Knowledge App\\Code\\watson-webservice-bank\\docs";

    private JSONObject makeSummaryFromText(int sentnum, String query, String doc, String docName) {
        JSONObject jsonelement = new JSONObject();
        Structurer structurer = new Structurer();
        Weighter weighter = new Weighter();
        Aggregator aggregator = new Aggregator();
        Summarizer summarizer = new Summarizer(structurer, weighter, aggregator);
        summarizer.setNumSentences(sentnum);
        summarizer.setQuery(query);
        summarizer.setTitle(query);
        String summary = null;
        try {
            summary = summarizer.summarize(doc);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(doc);
        }
        jsonelement.put("id", docName);
        jsonelement.put("title", docName);
        jsonelement.put("url", docName);
        jsonelement.put("summary", summary);
        jsonelement.put("searchTerm", query);
        return jsonelement;
    }

    private JSONArray processLocalFile(String[] keys, String fileName, int sentnum) throws IOException {
        JSONArray elements = new JSONArray();
        File textFile = new File(BASE_PATH, fileName);
        List<String> lines = FileUtils.readLines(textFile);
        logger.info("Lines to process: " + lines.size());
        String fullDoc = FileUtils.readFileToString(textFile, "UTF-8");
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            JSONObject jsonelement = new JSONObject();
            jsonelement = makeSummaryFromText(sentnum, key, fullDoc, fileName);
            jsonelement.put("lines", lines.size());
            elements.put(jsonelement);
        }
        return elements;
    }

    @GET
    @Path("/summarizeDoc")
    @Produces(MediaType.APPLICATION_JSON)
    public Response summarizeDoc(
            @QueryParam(value = "docName") @DefaultValue("how_to_raise_household_savings_in_LAC.txt") String docName,
            @QueryParam(value = "query") String query) {
        if (validateParam(query)) {
            String[] queries = query.split(",");

            if (queries.length > 3) {
                return Response.status(Response.Status.BAD_REQUEST).entity("The app allows a maximum of three queries at once").type("text/plain").build();
            } else {
                JSONObject result = new JSONObject();
                JSONArray elements = new JSONArray();
                try {
                    elements = processLocalFile(queries, docName, 5);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                JSONObject info = new JSONObject();
                info.put("document", docName);
                info.put("size", FileUtils.sizeOf(new File(BASE_PATH, docName)));
                result.put("info", info);
                result.put("elements", elements);
                return Response.status(200).entity(result.toString()).build();
            }

        } else {
            return wrongOrMissingParameterError("query", query);
        }
    }
}
