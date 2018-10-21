package com.company;

import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.ls.*;
import org.xml.sax.SAXException;

public class Article {
    private URL url;
    private DocumentBuilder docBuilder;
    private Document dom;
    private long domTime = 0;

    static int RELOAD_DELAY = 1000; // en millisecondes

    public Article(URL url) {
        this.url = url;
        domTime = 0;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException ex) {
            // évite d'avoir à déclarer 'throws ParserConfigurationException'
            throw new RuntimeException(ex);
        }
    }

    protected void refresh() throws IOException, SAXException {
        // évite de refaire la requête GET trop peu de temps avant la précédente
        long currentTime = System.currentTimeMillis();
        if (currentTime - domTime < RELOAD_DELAY) return;

        //System.err.println("--- refresh " + url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("accept", "application/xml");
        conn.connect();

        if (conn.getResponseCode() / 100 != 2) {
            throw new RuntimeException("Server replied: " + conn.getResponseMessage());
        }

        dom = docBuilder.parse(conn.getInputStream(), url.toString());
        domTime = System.currentTimeMillis();
    }

    protected void update() throws IOException, SAXException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.addRequestProperty("content-type", "application/xml");
        conn.addRequestProperty("accept", "application/xml");
        conn.setDoOutput(true);
        OutputStream stream = conn.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(stream);
        writer.write(get_xml());
        writer.close();
        conn.connect();

        if (conn.getResponseCode() / 100 != 2) {
            throw new RuntimeException("Server replied: " + conn.getResponseMessage());
        }

        dom = docBuilder.parse(conn.getInputStream(), url.toString());
    }

    protected String get_xml() {
        DOMImplementationLS domImplementation = (DOMImplementationLS) dom.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
        return lsSerializer.writeToString(dom);
    }

    public URL getURL() {
        return this.url;
    }

    public String getTitle() throws IOException, SAXException {
        refresh();
        return dom.getElementsByTagName("title").item(0).getTextContent();
    }

    public String getBody() throws IOException, SAXException {
        refresh();
        return dom.getElementsByTagName("body").item(0).getTextContent();
    }

    public String getDate() throws IOException, SAXException {
        refresh();
        return dom.getElementsByTagName("date").item(0).getTextContent();
    }

    public void setTitle(String newTitle) throws IOException, SAXException {
        domTime = 0; // force le refresh ci-dessous à vraiment charger les données
        refresh();    // pour être sûr qu'on a la dernière version du body
        dom.getElementsByTagName("title").item(0).setTextContent(newTitle);
        update();
    }

    public void setBody(String newBody) throws IOException, SAXException {
        domTime = 0; // force le refresh ci-dessous à vraiment charger les données
        refresh();    // pour être sûr qu'on a la dernière version du title
        dom.getElementsByTagName("body").item(0).setTextContent(newBody);
        update();
    }

    public void delete() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.addRequestProperty("accept", "application/xml");
        conn.connect();

        if (conn.getResponseCode() / 100 != 2) {
            throw new RuntimeException("Server replied: " + conn.getResponseMessage());
        }
    }
}