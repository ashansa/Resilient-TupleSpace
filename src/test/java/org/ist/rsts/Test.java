package org.ist.rsts;

import org.ist.rsts.tuple.Tuple;
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
import java.util.Random;

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

        server1.createServerGroup("config/server-modified.xml", "1");
        server2.createServerGroup("config/server-modified.xml", "2");
        server3.createServerGroup("config/server-modified.xml", "3");

        System.out.println("Starting servers");

        server1.start();
        server2.start();
        server3.start();

        System.out.println("Started servers");

       Thread.sleep(5000);

        /*WriteTest writeTest = new WriteTest(new ServerGroup[]{server1, server2, server3});
        writeTest.start();
        System.out.println("....... write test started .........");
        ReadTest readTest = new ReadTest(new ServerGroup[]{server1, server2, server3});
        readTest.start();
        System.out.println("....... read test started .........");

        writeTest.join();
        readTest.join();
*/

        WriteTest writeTest = new WriteTest(new ServerGroup[]{server1, server2, server3});
        writeTest.start();
        System.out.println("....... write test started .........");
        TakeTest takeTest = new TakeTest(new ServerGroup[]{server1, server2, server3});
        takeTest.start();
        System.out.println("....... take test started .........");

        //writeTest.join();
        //takeTest.join();

        Thread.sleep(3000);

        Thread readTest =new ReadTest(new ServerGroup[]{server1, server2, server3});
        readTest.start();
        readTest.join();

       /* System.out.println("huuuuuu");
        for (int i = 0; i < 100; i++) {
           server1.getClient().sendWriteRequest("1", "2", String.valueOf(i));
           // server1.getClient().sendReadRequest("*","*","*");;
            System.out.println("Writing tuple");
        }
        Thread.sleep(1000);

        for (int i = 0; i < 100; i++) {
           // server1.getClient().sendWriteRequest("1","2",String.valueOf(i));
            server1.getClient().sendReadRequest("*","*","*");;
            System.out.println("Reading tuple");
        }

        Thread.sleep(2000);
*/
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

    public class WriteTest extends Thread {

        ServerGroup[] servers;
        Random rand = new Random();
        public WriteTest(ServerGroup[] servers) {
            this.servers = servers;
        }
        public void run() {
            for (int i = 0; i < 100; i++) {
                try {
                int no = rand.nextInt(2);
                    servers[no].getClient().sendWriteRequest("1", "2", String.valueOf(i));
                     // server1.getClient().sendReadRequest("*","*","*");;
                    System.out.println("Writing tuple");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ReadTest extends Thread {

        ServerGroup[] servers;
        Random rand = new Random();
        public ReadTest(ServerGroup[] servers) {
            this.servers = servers;
        }
        public void run() {
            for (int i = 0; i < 1; i++) {
                try {
                    int no = rand.nextInt(2);
                   // servers[no].getClient().sendReadRequest("*", "*", "*");
                    Tuple tuple =    servers[no].read(new Tuple("*", "*", "*"));
                    // server1.getClient().sendReadRequest("*","*","*");;
                    System.out.println("Read tuple");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class TakeTest extends Thread {

        ServerGroup[] servers;
        Random rand = new Random();
        public TakeTest(ServerGroup[] servers) {
            this.servers = servers;
        }
        public void run() {
            for (int i = 0; i < 99; i++) {
                try {
                    int no = rand.nextInt(2);
                    servers[no].getClient().sendTakeRequest("*", "*", String.valueOf(i));

                    // server1.getClient().sendReadRequest("*","*","*");;
                    System.out.println("Take tuple");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
