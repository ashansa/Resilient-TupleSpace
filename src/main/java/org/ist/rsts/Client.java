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

public class Client extends Thread {

    private ServerGroup server;
    private Lock lock = new ReentrantLock();
    private Condition readBlock = lock.newCondition();
    private Condition takeBlock = lock.newCondition();

    public Client(ServerGroup server) {
        this.server = server;
    }

    public void sendWriteRequest(String value1, String value2, String value3) throws IOException {
        server.write(new Tuple(value1, value2, value3));
    }

    public void sendReadRequest(String value1, String value2, String value3) throws IOException, InterruptedException {
        try{
            lock.lock();
            Tuple result = server.read(new Tuple(value1, value2, value3));
            if(result == null) {
                System.out.println("Read Request: waiting..........");
                readBlock.await();
                System.out.println("Read Request: wait finish.......");
            } else {
                String[] values = result.getValues();
                System.out.println("================== RESULTS ====================");
                System.out.println(values[0] + "," + values[1] + "," + values[2]);
                System.out.println("===============================================");
            }
        } finally {
            lock.unlock();
        }
    }

    public void sendTakeRequest(String value1, String value2, String value3) throws IOException, InterruptedException {
        try{
            lock.lock();
            System.out.println("take request: wait.....");
            server.take(new Tuple(value1, value2, value3));
            takeBlock.await();
            System.out.println("take request: wait finish.......");
        } finally {
            lock.unlock();
        }
    }

    public void receiveResults(Tuple result, Type type) {

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

        System.out.println("================== RECEIVED RESULTS ====================");
        String[] values = result.getValues();
        System.out.println(values[0] + "," + values[1] + "," + values[2]);
        System.out.println("========================================================");
    }

    private void isolateNode() {
        server.isolate();
    }

    private void recoverNode() {
        server.recover();
    }

    public void run() {

        String line = null;
        System.out.println("\n \n \n ################### starting RSTS Client ###########################");
        System.out.println("eg: write:a,b,c");
        System.out.println("    read:a,*,*");
        System.out.println("    take:a,*,*");
        System.out.println("To isolate (or recover) node - type isolate (recover)");

        while (true) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        System.in));
                line = br.readLine();

                if (line != null) {

                    if (line.contains("isolate")) {
                        isolateNode();
                    } else  if (line.contains("recover")) {
                        recoverNode();
                    } else {
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
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

    }
}
