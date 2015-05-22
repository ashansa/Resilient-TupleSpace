package org.ist.rsts;

import org.junit.Assert;
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
import java.util.ArrayList;
import java.util.Random;

public class Test1 {

    long takeTestTime;
    long writeTestTime;

    int noOfNodes = 10;
    ArrayList<ServerGroup> servers = new ArrayList<ServerGroup>();
    private long readTestTime;

   /* public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        new Test().createConfig();
    }*/

    @org.junit.Test
    public void testCreateGroup() throws Exception {

        System.out.println("created config");
        //GossipServer server = new GossipServer();
        createConfig();

        System.out.println("Starting servers");
        for (int i = 0; i < noOfNodes; i++) {
            ServerGroup server = new ServerGroup();
            servers.add(server);
            server.createServerGroup("config/server-modified.xml", String.valueOf(i + 1));
            server.start();
        }

        /*ServerGroup server1 = new ServerGroup();
        ServerGroup server2 = new ServerGroup();
        ServerGroup server3 = new ServerGroup();*/

       /* server1.createServerGroup("config/server-modified.xml", "1");
        server2.createServerGroup("config/server-modified.xml", "2");
        server3.createServerGroup("config/server-modified.xml", "3");
*/


       /* server1.start();
        server2.start();
        server3.start();*/

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

        WriteTest writeTest = new WriteTest(servers);
        writeTest.start();
        System.out.println("....... write test started .........");
        TakeTest takeTest = new TakeTest( servers);
        //takeTest.start();
        System.out.println("....... take test started .........");

        ReadTest readTest = new ReadTest( servers);
        readTest.start();
        System.out.println("....... read test started .........");

        writeTest.join();
        //takeTest.join();

        readTest.join();

        while (servers.get(0).getTupleSpace().tupleSize()!=100){
        }


        System.out.println("=====================================================");
        readTest.run();

        System.out.println("time for writes " + writeTestTime + "-" + writeTestTime / 100);
        System.out.println("time for takes "+takeTestTime +"-"+takeTestTime/50);
        System.out.println("time for read " + readTestTime + "-" + readTestTime / 100);

       for (int i = 0; i <noOfNodes ; i++) {
            Assert.assertEquals(100, servers.get(i).getTupleSpace().tupleSize());
            System.out.println("Tuple size in Server "+i +" "+ servers.get(i).getTupleSpace().tupleSize());
        }






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
        if (doctype != null) {
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        }


        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File("config/server-modified.xml"));
        transformer.transform(source, result);
    }

    public class WriteTest extends Thread {

        ArrayList<ServerGroup> servers;
        Random rand = new Random();

        public WriteTest(ArrayList<ServerGroup> servers) {
            this.servers = servers;
        }

        public void run() {
            long startTime = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                try {
                    int no = rand.nextInt(noOfNodes);
                    servers.get(no).getClient().sendWriteRequest("1", "2", String.valueOf(i));
                    //servers[no].write(new Tuple("1", "2", String.valueOf(i)));
                    // server1.getClient().sendReadRequest("*","*","*");;
                    System.out.println("Writing tuple");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long endTime = System.nanoTime();
            writeTestTime = endTime - startTime;
        }
    }

    public class ReadTest extends Thread {

        ArrayList<ServerGroup> servers;
        Random rand = new Random();

        public ReadTest(ArrayList<ServerGroup> servers) {
            this.servers = servers;
        }

        public void run() {
            long startTime = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                try {
                    int no = rand.nextInt(noOfNodes);
                     servers.get(no).getClient().sendReadRequest("*", "*", "*");
                    //Tuple tuple = servers.get(no).read(new Tuple("*", "*", "*"));
                    // server1.getClient().sendReadRequest("*","*","*");;
                    //System.out.println("Read tuple =========>" + tuple.getValues());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long endTime = System.nanoTime();
            readTestTime = endTime - startTime;
        }
    }

    public class TakeTest extends Thread {

        ArrayList<ServerGroup> servers;
        Random rand = new Random();

        public TakeTest(ArrayList<ServerGroup> servers) {
            this.servers = servers;
        }

        public void run() {

            long startTime = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                try {
                    int no = rand.nextInt(noOfNodes);
                    servers.get(no).getClient().sendTakeRequest("*", "*", String.valueOf(i));
                    //servers[no].take(new Tuple("*", "*", String.valueOf(i)));
                    //Thread.sleep(500);
                    // server1.getClient().sendReadRequest("*","*","*");;
                    //System.out.println("Take tuple");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long endTime = System.nanoTime();
            takeTestTime = endTime - startTime;
        }
    }
}
