/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this logFile except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * Developer(s): Nuno Carvalho.
 */

package org.ist.rsts;

import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.*;
import net.sf.jgcs.membership.*;
import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.TupleManager;
import org.ist.rsts.tuple.TupleMessage;
import org.ist.rsts.tuple.Type;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

/**
 * This class defines a ServerOpenGroupTest. This example shows how to use and
 * configure Appia with jGCS using an open group, where there is a group of
 * servers that accept Messages from external members. This is the server part.
 * <p/>
 * The example only shows how to configure and use, and it only sends dummy
 * messages. It does not intend to implement any algorithm.
 *
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ServerGroup extends Thread implements ControlListener, ExceptionListener,
        MembershipListener, BlockListener {

    private long viewChangeTime = 0;

    private ControlSession control;
    private DataSession groupSession;
    private Service group;
    //TupleSpace tupleSpace;
    private TupleManager tupleManager;
    private LogManager logManager;
    private Client client;

    Properties properties = new Properties();
    int membersInGroup = -1;
    int allNodes = -1;
    boolean isBlocked = false;
    static int writeTakeSeqNo = 0;

    private SocketAddress localAddress;
    public static boolean isIsolated = false;

    private void init(ControlSession control, DataSession grSession, Service gr, String logId) throws IOException {
        this.control = control;
        this.groupSession = grSession;
        this.group = gr;
        this.tupleManager = new TupleManager(this);
        this.logManager = new LogManager(logId);

        StateManager.getInstance().init(grSession, gr, tupleManager, logManager, this);

        InputStream input = new FileInputStream("./src/main/java/services.properties");
        properties.load(input);
        allNodes = Integer.parseInt(properties.getProperty("number_of_nodes"));
        System.out.println(" all nodes =======> " + allNodes);

        System.out.println("Group is " + gr);

        // set listeners
        GroupMessageListener l = new GroupMessageListener();
        groupSession.setMessageListener(l);
        groupSession.setServiceListener(l);
        control.setControlListener(this);
        control.setExceptionListener(this);
        if (control instanceof MembershipSession)
            ((MembershipSession) control).setMembershipListener(this);
        if (control instanceof BlockSession)
            ((BlockSession) control).setBlockListener(this);
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Must put the xml logFile name as an argument.");
            System.exit(1);
        }

        try {
            ServerGroup serverGroup = new ServerGroup();
            serverGroup.createServerGroup(args[0], args[1]);
            serverGroup.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createServerGroup(String configFile, String logId) throws IOException {
        ProtocolFactory protocolFactory = new AppiaProtocolFactory();
        AppiaGroup appiaGroup = new AppiaGroup();
        appiaGroup.setGroupName("group");
        appiaGroup.setConfigFileName(configFile);
        Protocol protocol = protocolFactory.createProtocol();
        DataSession session = protocol.openDataSession(appiaGroup);
        ControlSession control = protocol.openControlSession(appiaGroup);
        Service sg = new AppiaService("rrpc_group");
        this.init(control, session, sg, logId);
    }

    public void run() {

        //starting the client thread
        client = new Client(this);
        getClient().start();

        try {
            // joins the group
            control.join();

            // wait forever.
            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            //log here
            e.printStackTrace();
        }
    }

    public void write(Tuple tuple) {
        System.out.println("all and current : " + allNodes + " , " + membersInGroup);
        if (membersInGroup > Math.ceil(allNodes / 2)) {

            if (isBlocked)
                System.out.println("........... operations are blocked. Waiting until unblocked to write.......");

            //Avoid sending messages after Block OK is issued
            while (isBlocked) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("........... operations NOT blocked. Going to write.......");

            TupleMessage msg = new TupleMessage(tuple, Type.WRITE);
            sendClientRequest(msg);
        } else {
            System.out.println("You are in a minority partition. Cannot execute write request.");
        }
    }

    public Tuple read(Tuple template) {

        String[] tupleValues = template.getValues();
        //read can be served locally without bcast request
        Tuple tuple = tupleManager.readTuple(new Tuple(tupleValues[0], tupleValues[1], tupleValues[2]));

        //tuple can be null if no match found
        return tuple;

    }

    public void take(Tuple template) {
       /* System.out.println("all and current : " + allNodes + " , " + membersInGroup);
        if (membersInGroup > Math.ceil(allNodes / 2)) {

            if (isBlocked)
                System.out.println("........... operations are blocked. Waiting until unblocked to take.......");

            //Avoid sending messages after Block OK is issued
            while (isBlocked) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("........... operations NOT blocked. Going to take.......");

            Tuple tupleToTake = tupleManager.getTupleForTake(template, false);
            if (tupleToTake != null) {
                TupleMessage msg = new TupleMessage(tupleToTake, Type.TAKE);
                sendClientRequest(msg);
            }
        } else {
            System.out.println("You are in a minority partition. Cannot execute write request.");
        }*/
    }

    public void take2(Tuple template) {
        System.out.println("all and current : " + allNodes + " , " + membersInGroup);
        if (membersInGroup > Math.ceil(allNodes / 2)) {

            if (isBlocked)
                System.out.println("........... operations are blocked. Waiting until unblocked to take.......");

            //Avoid sending messages after Block OK is issued
            while (isBlocked) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("........... operations NOT blocked. Going to take.......");

            TupleMessage msg = new TupleMessage(template, Type.TAKE2);
            sendClientRequest(msg);

        } else {
            System.out.println("You are in a minority partition. Cannot execute write request.");
        }
    }

    private void takeWithBroadcast(Tuple template) {
        Vector<Tuple> matches = tupleManager.getMatchingTuples(template);

    }

    public void receiveTakeDecisionResult(Tuple tupleToTake) {
        TupleMessage msg = new TupleMessage(tupleToTake, Type.TAKE);
        sendClientRequest(msg);
    }

    public void isolate() {
        System.out.println("isolate recieved at Server");
        isIsolated = true;
        //sendRequest(Constants.MessageType.ISOLATE);
    }

    public void recover() {
        System.out.println("recover recieved at Server");
        isIsolated = false;
        //sendRequest(Constants.MessageType.RECOVER);
    }

    public int getMembersInGroup() {
        return membersInGroup;
    }

    private void sendClientRequest(TupleMessage tupleMsg) {
        try {
            Message msg = groupSession.createMessage();
            tupleMsg.marshal();
            byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, tupleMsg.getByteArray());
            msg.setPayload(bytes);
            System.out.println("====== sending client request : BCAST......");
            groupSession.send(msg, group, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendResultsNotificationToClient(Tuple tuple, Type type) throws IOException {
        getClient().receiveResults(tuple, type);
        /*Message reply = groupSession.createMessage();
        TupleMessage tupleMessage = new TupleMessage(tuple, Type.REPLY);
        tupleMessage.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, tupleMessage.getByteArray());
        reply.setPayload(bytes);
        groupSession.send(reply, clients, null, serverMessage.addr);*/
    }

    public void bcastMatchingTuplesForTake(Vector<Tuple> matchingTuples, UUID uuid) {
        try {
            TakeResponseMessage takeResponseMessage = new TakeResponseMessage(matchingTuples, uuid);
            takeResponseMessage.marshal();
            byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TAKE_RESPONSE, takeResponseMessage.getByteArray());

            Message msg = groupSession.createMessage();
            msg.setPayload(bytes);
            groupSession.send(msg, group, null, null);
        } catch (ClosedSessionException e) {
            e.printStackTrace();
        } catch (UnsupportedServiceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onJoin(SocketAddress peer) {
        System.out.println("-- JOIN: " + peer);
    }

    public void onLeave(SocketAddress peer) {
        System.out.println("-- LEAVE: " + peer);
    }

    public void onFailed(SocketAddress peer) {
        System.out.println("-- FAILED: " + peer);
    }

    public void onMembershipChange() {
        //System.out.println("MEMBERSHIP: "
        //		+ (System.currentTimeMillis() - viewChangeTime));
        try {
            System.out.println("-- NEW VIEW: " + ((MembershipSession) control).getMembership().getMembershipID() +
                    "\tSize: " + ((MembershipSession) control).getMembership().getMembershipList().size());

            int noOfMembers = ((MembershipSession) control).getMembership().getMembershipList().size();
            Membership membership = ((MembershipSession) control).getMembership();
            String viewIdString = membership.getMembershipID().toString().split(";")[0].split(":")[1];
            int localId = ((MembershipSession) control).getMembership().getLocalRank();
            localAddress = ((MembershipSession) control).getMembership().getMemberAddress(localId);
            System.out.println("My local Adress is ==========>>>>>>>" + localAddress);
            int newViewId = 0;

            if (noOfMembers > Math.ceil(allNodes / 2) && viewIdString != null) { // sync view only if you are in the majority view
                System.out.println("new view id string : " + viewIdString);
                newViewId = Integer.valueOf(viewIdString);
                System.out.println("View ID: " + newViewId);
                try {
                    System.out.println("current view Id : " + StateManager.getInstance().getCurrentViewId());
                    System.out.println("new view Id : " + newViewId);
                    StateManager.getInstance().sync(((MembershipSession) control).getMembership().getMembershipList(), newViewId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            StateManager.getInstance().setViewNumber(newViewId);
            System.out.println("now view Id : " + StateManager.getInstance().getCurrentViewId());

            membersInGroup = ((MembershipSession) control).getMembership().getMembershipList().size();
            isBlocked = false;
        } catch (NotJoinedException e) {
            e.printStackTrace();
           // groupSession.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // this notification is issued before a new view
    // a new view will not appear while the flush is not notified
    // (using the blockOk() method). After this, no message can be sent
    // while waiting for a new view.
    public void onBlock() {
        viewChangeTime = System.currentTimeMillis();
        System.out.println("Asking to BLOCK");
        try {
            isBlocked = true;
            ((BlockSession) control).blockOk();
        } catch (JGCSException e) {
            e.printStackTrace();
        }
    }

    public void onExcluded() {
        System.out.println("-- EXCLUDED");
    }

    public void onException(JGCSException arg0) {
        System.out.println("-- EXCEPTION: " + arg0.getMessage());
        arg0.printStackTrace();
    }

    public Client getClient() {
        return client;
    }

    /*
     * Class that implements a message listener
	 */
    private class GroupMessageListener implements MessageListener,
            ServiceListener {

        /*
         * All messages arrive here. Messages can be sent from clients or
         * servers. Messages from servers are totally ordered and messages from
         * clients arrive async. from another communication channel.
         */
        public Object onMessage(Message msg) {
            ProtocolMessage protoMsg = null;
            try {
                protoMsg = Constants.createMessageInstance(msg.getPayload());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (protoMsg == null)
                return null;

            if (protoMsg instanceof TupleMessage) {
                System.out.println("######### TupleMessage #############");
                handleRquest((TupleMessage) protoMsg, msg.getSenderAddress());
                return null;

            } else if (protoMsg instanceof LogRequestMessage) {

                System.out.println("######### LogRequestMessage #############");
                try {
                    StateManager.getInstance().sendLogsToMerge((LogRequestMessage) protoMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (protoMsg instanceof LogResponseMessage) {
                System.out.println("######### LogResponseMessage #############");
                try {
                    protoMsg.unmarshal();
                    StateManager.getInstance().addToBlockingQueue((LogResponseMessage) protoMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else if(protoMsg instanceof TakeResponseMessage) {
                System.out.println("*********** take response received ********");
                try {
                    protoMsg.unmarshal();
                    tupleManager.addTakeResponse((TakeResponseMessage) protoMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (protoMsg instanceof ServerMessage) {
                System.out.println("========server msg received OnMessage=======");
                /*handleServerMessage((ServerMessage) protoMsg,
                        msg.getSenderAddress());*/
                return null;
            }
            return null;
        }

        public void onServiceEnsured(Object context, Service service) {
            // try {
            // if(service.compare(uniform)>=0){
            // handleServerMessage((Message) context);
            // }
            // } catch (UnsupportedServiceException e) {
            // e.printStackTrace();
            // }
        }

        private void handleRquest(TupleMessage tupleMessage, SocketAddress addr) {
            try {
                tupleMessage.unmarshal();

                if (addr.equals(control.getLocalAddress())) {
                    System.out.println("\tReceived request message. I'm the origin)");
                    System.out.println("tuple msg type in request : " + tupleMessage.getType());
                } else {
                    System.out.println("\tReceived request message");
                }
                System.out.println("Type: " + tupleMessage.getType());

                String[] tupleValues = tupleMessage.getTuple().getValues();
                switch (tupleMessage.getType()) {
                    case WRITE:
                        //tupleManager.writeTuple(new Tuple(tupleValues[0], tupleValues[1], tupleValues[2]));
                        tupleManager.writeTuple(tupleMessage.getTuple());
                        System.out.println("Writing to the log");
                        logManager.writeLog(tupleMessage, StateManager.getInstance().getCurrentViewId());
                        break;
                    case TAKE:
                        /*Tuple tuple = tupleManager.takeTuple(tupleMessage.getTuple());
                        if (tuple != null) {
                            sendResultsNotificationToClient(tuple, Type.TAKE);
                        }
                        System.out.println("Writing to the log");
                        logManager.writeLog(tupleMessage, StateManager.getInstance().getCurrentViewId());*/
                        break;
                    case TAKE2:
                        tupleManager.addToTakeQueue(tupleMessage);
                        break;

                    //case READ not needed
                    // because getMatchingTuples request can be served locally. So other servers will not get the getMatchingTuples request
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } // end of class GroupMessageListener

}
