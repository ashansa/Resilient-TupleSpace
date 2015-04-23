/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Developer(s): Nuno Carvalho.
 */

package org.ist.rsts;

import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class defines a ClientOpenGroupTest This example shows how to use and
 * configure Appia with jGCS using an open group, where there is a group of
 * servers that accept Messages from external members. This is the (external)
 * client part.
 * <p/>
 * The example only shows how to configure and use, and it only sends dummy
 * messages. It does not intend to implement any algorithm.
 *
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class Client extends Thread {

    private int id = 0;
    private int lastReceivedMessage = -1;
    private ServerGroup server;

    public Client(ServerGroup server) {
        this.server = server;
    }

    private void sendMessage(String value1, String value2, String value3, Type type) throws IOException {
        /*Message msg = data.createMessage();
        TupleMessage tupleMessage = new TupleMessage(id++, new Tuple(value1, value2, value3), type);
        tupleMessage.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, tupleMessage.getByteArray());
        msg.setPayload(bytes);
        data.send(msg, rpcService, null, null);*/
    }

    private void sendWriteRequest(String value1, String value2, String value3) throws IOException {
        server.write(new Tuple(value1, value2, value3));
    }

    private void sendReadRequest(String value1, String value2, String value3) throws IOException {
        server.read(new Tuple(value1, value2, value3));
    }

    public void receiveResults(Tuple result) {
        System.out.println("result received");
        for (String value : result.getValues()) {
            System.out.println(value);
        }
    }

    public void run() {

        System.out.println("> \n");
        String line = null;

        System.out.println("eg: write:1,2,3");
        System.out.println("    read:*,2,3");
        System.out.println("    take:*,2,3");

        try{
            while (true) {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        System.in));
                line = br.readLine();

                if (line.contains("write")) {
                    System.out.println("Sending tuple write request");
                    //splitting the line write:1,2,3
                    String[] values = line.split(":")[1].split(",");
                    sendWriteRequest(values[0], values[1], values[2]);

                } else if (line.contains("read")) {
                    System.out.println("Sending tuple read request");
                    //splitting the line write:1,2,3
                    String[] values = line.split(":")[1].split(",");
                    sendReadRequest(values[0], values[1], values[2]);

                } else if (line.contains("take")) {
                    System.out.println("Not implemented yet");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
