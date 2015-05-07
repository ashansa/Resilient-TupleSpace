/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this logFile except in compliance with the License.
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private Lock lock = new ReentrantLock();
    private Condition readBlock = lock.newCondition();
    private Condition takeBlock = lock.newCondition();

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

    public void sendWriteRequest(String value1, String value2, String value3) throws IOException {
        server.write(new Tuple(value1, value2, value3));
    }

    public void sendReadRequest(String value1, String value2, String value3) throws IOException, InterruptedException {
        try{
            lock.lock();
            Tuple result = server.read(new Tuple(value1, value2, value3));
            if(result == null) {
                System.out.println("read: waiting..........");
                readBlock.await();
                System.out.println("read: wait finish.......");
            } else {
                String[] values = result.getValues();
                System.out.println("result :" + values[0] + "," + values[1] + "," + values[2]);
            }

        } finally {
            lock.unlock();
        }

    }

    public void sendTakeRequest(String value1, String value2, String value3) throws IOException, InterruptedException {
        try{
            lock.lock();
            server.take(new Tuple(value1, value2, value3));
            System.out.println("take : wait.....");
            takeBlock.await();
            System.out.println("take : wait finish.......");
        } finally {
            lock.unlock();
        }

    }


    public void receiveResults(Tuple result, Type type) {
        System.out.println("result received");
        for (String value : result.getValues()) {
            System.out.println(value);
        }

        if(Type.READ.equals(type)) {
            try{
                lock.lock();
                readBlock.signal();
                System.out.println("read signaled......");
            } finally {
                lock.unlock();
            }
        }
        if(Type.TAKE.equals(type)) {
            try{
                lock.lock();
                takeBlock.signal();
                System.out.println("take signaled......");
            } finally {
                lock.unlock();
            }

        }
    }

    public void run() {

        System.out.println("> \n");
        String line = null;

        System.out.println("eg: write:1,2,3");
        System.out.println("    read:*,2,3");
        System.out.println("    take:*,2,3");

        while (true) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        System.in));
                line = br.readLine();

                if (line != null) {
                    //splitting the line write:1,2,3
                    String[] values = line.split(":")[1].split(",");

                    if (line.contains("write")) {
                        System.out.println("Sending tuple write request");
                        sendWriteRequest(values[0], values[1], values[2]);

                    } else if (line.contains("read")) {
                        System.out.println("Sending tuple read request");
                        try {
                            sendReadRequest(values[0], values[1], values[2]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } else if (line.contains("take")) {
                        try {
                            sendTakeRequest(values[0], values[1], values[2]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

    }
}
