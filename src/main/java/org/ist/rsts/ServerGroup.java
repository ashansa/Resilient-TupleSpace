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

import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.*;
import net.sf.jgcs.membership.BlockListener;
import net.sf.jgcs.membership.BlockSession;
import net.sf.jgcs.membership.MembershipListener;
import net.sf.jgcs.membership.MembershipSession;
import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.TupleMessage;
import org.ist.rsts.tuple.TupleSpace;
import org.ist.rsts.tuple.Type;

import java.io.IOException;
import java.net.SocketAddress;
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
    TupleSpace tupleSpace;
    private Client client;
    ServerGroup test;


    private void init(ControlSession control, DataSession grSession, Service gr) throws JGCSException {
        this.control = control;
        this.groupSession = grSession;
        this.group = gr;
        this.tupleSpace = new TupleSpace();

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

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Must put the xml file name as an argument.");
            System.exit(1);
        }

        try {
            ServerGroup serverGroup = new ServerGroup();
            serverGroup.createServerGroup(args[0]);
            serverGroup.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createServerGroup(String configFile) throws JGCSException {
        ProtocolFactory protocolFactory = new AppiaProtocolFactory();
        AppiaGroup appiaGroup = new AppiaGroup();
        appiaGroup.setGroupName("group");
        appiaGroup.setConfigFileName(configFile);
        Protocol protocol = protocolFactory.createProtocol();
        DataSession session = protocol.openDataSession(appiaGroup);
        ControlSession control = protocol.openControlSession(appiaGroup);
        Service sg = new AppiaService("rrpc_group");
        this.init(control, session, sg);
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
        TupleMessage msg = new TupleMessage(tuple, Type.WRITE);
        sendClientRequest(msg);
    }

    public void read(Tuple template) {
        try {
            String[] tupleValues = template.getValues();
            Vector<Tuple> tuple = tupleSpace.read(new Tuple(tupleValues[0], tupleValues[1], tupleValues[2]));
            if (tuple.size() > 0) {
                sendResultsToClient(tuple.firstElement());
                String[] result = tuple.firstElement().getValues();
                System.out.println("result :" + result[0] + "," + result[1] + "," + result[2]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void take(Tuple template) {
        TupleMessage msg = new TupleMessage(template, Type.TAKE);
        sendClientRequest(msg);
    }

    private void sendClientRequest(TupleMessage tupleMsg) {
        try {
            Message msg = groupSession.createMessage();
            tupleMsg.marshal();
            byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, tupleMsg.getByteArray());
            msg.setPayload(bytes);
            groupSession.send(msg, group, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendResultsToClient(Tuple tuple) throws IOException {
        getClient().receiveResults(tuple);
        /*Message reply = groupSession.createMessage();
        TupleMessage tupleMessage = new TupleMessage(tuple, Type.REPLY);
        tupleMessage.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, tupleMessage.getByteArray());
        reply.setPayload(bytes);
        groupSession.send(reply, clients, null, serverMessage.addr);*/
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
            System.out.println("-- NEW VIEW: "
                    + ((MembershipSession) control).getMembership().getMembershipID() + "\tSize: " + ((MembershipSession) control).getMembership().getMembershipList().size());
        } catch (NotJoinedException e) {
            e.printStackTrace();
            groupSession.close();
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
                System.out.println("========tuple msg received OnMessage=======");
                handleRquest((TupleMessage) protoMsg, msg.getSenderAddress());
                return null;
            }

            // If if is a client message
            else if (protoMsg instanceof ServerMessage) {
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
                        tupleSpace.write(new Tuple(tupleValues[0], tupleValues[1], tupleValues[2]));
                        break;
                    case TAKE:
                        break;
                    //case READ not needed
                    // because read request can be served locally. So other servers will not get the read request
                }
                System.out.println("current tuple size: " + tupleSpace.tupleSize());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } // end of class GroupMessageListener

}
