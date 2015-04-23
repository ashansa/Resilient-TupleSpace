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
import java.util.Hashtable;
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
public class ServerGroup implements ControlListener, ExceptionListener,
        MembershipListener, BlockListener {

    private long viewChangeTime = 0;

    private ControlSession control;
    private DataSession groupSession;
    private Service clients, group;

    TupleSpace tupleSpace;

    private Hashtable<Integer, Long> times = new Hashtable<Integer, Long>();


    public ServerGroup(ControlSession control, DataSession grSession,
                       Service cl, Service gr) throws JGCSException {
        this.control = control;
        this.groupSession = grSession;
        this.clients = cl;
        this.group = gr;
        this.tupleSpace = new TupleSpace();

        System.out.println("Group is " + gr);
        System.out.println("Client is " + cl);

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

    public void run() throws Exception {
        // joins the group
        control.join();

        // wait forever.
        Thread.sleep(Long.MAX_VALUE);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Must put the xml file name as an argument.");
            System.exit(1);
        }

        try {
            ProtocolFactory pf = new AppiaProtocolFactory();
            AppiaGroup g = new AppiaGroup();
            g.setGroupName("group");
            g.setConfigFileName(args[0]);
            Protocol p = pf.createProtocol();
            DataSession session = p.openDataSession(g);
            ControlSession control = p.openControlSession(g);

            Service sc = new AppiaService("rrpc");
            Service sg = new AppiaService("rrpc_group");

            ServerGroup test = new ServerGroup(control,
                    session, sc, sg);
            test.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Class that implements a message listener
	 */
    private class GroupMessageListener implements MessageListener,
            ServiceListener {

        Service uniform = new AppiaService("uniform_total_order");

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
                handleTupleMessage((TupleMessage) protoMsg,
                        msg.getSenderAddress());
                return null;
            }

            // If if is a client message
            else if (protoMsg instanceof ServerMessage) {
                handleServerMessage((ServerMessage) protoMsg,
                        msg.getSenderAddress());
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

        private void handleTupleMessage(TupleMessage msg, SocketAddress addr) {
            //TupleMessage
            try {
                msg.unmarshal();
                Message groupMsg = null;
                System.out.println("\tReceived a tuple msg: ");

                for (String value : msg.getTuple().getValues()) {
                    System.out.println("tuple val: " + value);
                }
                System.out.println();
                groupMsg = groupSession.createMessage();
                ServerMessage serverMsg = new ServerMessage(msg.getId(), addr, msg);

                serverMsg.marshal();
                byte[] bytes = Constants.createMessageToSend(
                        Constants.MessageType.SERVER, serverMsg.getByteArray());
                groupMsg.setPayload(bytes);

                times.put(msg.getId(), System.nanoTime());
                // System.out.println("added time for message #" + msg.id);

                // forward message to the servers, using the "group" Service
                //System.out.println("multicasting message to the group");
                groupSession.multicast(groupMsg, group, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleServerMessage(ServerMessage smsg, SocketAddress addr) {
            try {
                smsg.unmarshal();
                TupleMessage tupleMessage = smsg.tupleMessage;
                if (addr.equals(control.getLocalAddress())) {
                    System.out.println("\tReceived server message with id: " + smsg.id + " (I'm the origin)");
                    System.out.println("tuple msg type in server msg: " + tupleMessage.getType());
                } else {
                    System.out.println("\tReceived server message with id: " + smsg.id);
                }

                String[] tupleValues = tupleMessage.getTuple().getValues();
                switch (tupleMessage.getType()) {
                    case WRITE:
                        tupleSpace.write(new Tuple(tupleValues[0], tupleValues[1], tupleValues[2]));
                        break;
                    case READ:
                        Vector<Tuple> tuple = tupleSpace.read(new Tuple(tupleValues[0], tupleValues[1], tupleValues[2]));
                        if (tuple.size() > 0)
                            sendResultsToClient(smsg, tuple.firstElement());
                        break;
                    case TAKE:
                        break;
                }
                System.out.println("current tuple size: " + tupleSpace.tupleSize());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } // end of class GroupMessageListener

    private void sendResultsToClient(ServerMessage serverMessage, Tuple tuple) throws IOException {
        Message reply = groupSession.createMessage();
        TupleMessage tupleMessage = new TupleMessage(serverMessage.id, tuple, Type.REPLY);
        tupleMessage.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, tupleMessage.getByteArray());
        reply.setPayload(bytes);
        groupSession.send(reply, clients, null, serverMessage.addr);
    }

}
