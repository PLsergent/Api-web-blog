package com.company;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.ls.*;
import org.xml.sax.SAXException;

public class Blog {
    private URL url;
    private DocumentBuilder docBuilder;

    public Blog(URL url) {
        this.url = url;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException ex) {
            // évite d'avoir à déclarer 'throws ParserConfigurationException'
            throw new RuntimeException(ex);
        }
    }

    public URL getURL() {
        return url;
    }

    public Iterable<Article> iterArticles() throws IOException, SAXException {
        return iterArticles(null);
    }

    public Iterable<Article> iterArticles(String filter) throws IOException, SAXException {
        URL url;
        if (filter == null) {
            url = this.url;
        } else {
            String query = "?search=" + URLEncoder.encode(filter);
            url = new URL(this.url, query);
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("accept", "application/xml");
        conn.connect();

        if (conn.getResponseCode() / 100 != 2) {
            throw new RuntimeException("Server replied: " + conn.getResponseMessage());
        }
        LinkedList<Article> list = new LinkedList();

        Document doc = docBuilder.parse(conn.getInputStream(), url.toString());
        NodeList nodes = doc.getDocumentElement().getChildNodes();
        for (int i=0; i<nodes.getLength(); i+=1) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) continue;
            String href = n.getAttributes().getNamedItem("href").getTextContent();
            list.add(new Article(new URL(href)));
        }
        return list;
    }
    protected String get_xml(Document doc) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
        return lsSerializer.writeToString(doc);
    }

    public Article createArticle(String title, String body) throws IOException {
        Document doc = docBuilder.newDocument();
        Element articleElt = doc.createElement("article");
        Element titleElt = doc.createElement("title");
        Element bodyElt = doc.createElement("body");
        titleElt.setTextContent(title);
        bodyElt.setTextContent(body);
        articleElt.appendChild(titleElt);
        articleElt.appendChild(bodyElt);
        doc.appendChild(articleElt);

        String xml = get_xml(doc);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.addRequestProperty("content-type", "application/xml");
        conn.addRequestProperty("accept", "application/xml");
        conn.setDoOutput(true);
        OutputStream stream = conn.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(stream);
        writer.write(xml);
        writer.close();
        conn.connect();

        if (conn.getResponseCode() / 100 != 2) {
            throw new RuntimeException("Server replied: " + conn.getResponseMessage());
        }
        String location = conn.getHeaderField("location");
        return new Article(new URL(location));
    }
}
