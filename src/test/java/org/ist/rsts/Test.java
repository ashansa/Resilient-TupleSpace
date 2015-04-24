package org.ist.rsts;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class Test {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        new Test().createConfig();
    }

    @org.junit.Test
    public void testCreateGroup() throws Exception {

        //GossipServer server = new GossipServer();

        createConfig();

        ServerGroup server1 = new ServerGroup();
        ServerGroup server2 = new ServerGroup();
        ServerGroup server3 = new ServerGroup();

        server1.createServerGroup("config/server-modified.xml");
        server2.createServerGroup("config/server-modified.xml");
        server3.createServerGroup("config/server-modified.xml");

        server1.start();
        server2.start();
        server3.start();

       Thread.sleep(5000);

        System.out.println("huuuuuu");
        for (int i = 0; i < 100; i++) {
           server1.getClient().sendWriteRequest("1", "2", String.valueOf(i));
           // server1.getClient().sendReadRequest("*","*","*");;
            System.out.println("Writing tuple");
        }
        Thread.sleep(1000);

        for (int i = 0; i < 100; i++) {
           // server1.getClient().sendWriteRequest("1","2",String.valueOf(i));
            server1.getClient().sendReadRequest("*","*","*");;
            System.out.println("Writing tuple");
        }

        Thread.sleep(2000);

        //Thread.sleep(60000);

    }

    private void createConfig() throws IOException, ParserConfigurationException, SAXException, TransformerException {

        System.out.println(InetAddress.getLocalHost().getHostAddress());
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("config/server1.xml");

        Node gossipNode = doc.getElementsByTagName("parameter").item(0);
        gossipNode.getFirstChild().setNodeValue(InetAddress.getLocalHost().getHostAddress() + ":10000");
        //gossipAddress.setTextContent(InetAddress.getLocalHost().getHostAddress());

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DocumentType doctype = doc.getDoctype();
        if(doctype != null) {
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        }


    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(new File("config/server-modified.xml"));
    transformer.transform(source,result);
}
}
