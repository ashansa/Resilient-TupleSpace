package org.ist.rsts;

import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.*;
import net.sf.jgcs.membership.*;
import org.ist.rsts.tuple.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerGroup extends Thread implements ControlListener, ExceptionListener,
        MembershipListener, BlockListener {

    private ControlSession control;
    private DataSession groupSession;
    private Service group;
    private Service totalGroup;
    private TupleManager tupleManager;
    private LogManager logManager;
    private Client client;

    Properties properties = new Properties();
    int membersInGroup = -1;
    int allNodes = -1;
    boolean isBlocked = false;

    private SocketAddress localAddress;
    public static boolean isIsolated = false;

    private void init(ControlSession control, DataSession grSession, Service gr, Service total, String logId) throws IOException {
        this.control = control;
        this.groupSession = grSession;
        this.group = gr;
        this.totalGroup = total;
        this.logManager = new LogManager(logId);
        this.tupleManager = new TupleManager(this, logManager);

        StateManager.getInstance().init(grSession, gr, tupleManager, logManager, this);

        InputStream input = new FileInputStream("./src/main/java/services.properties");
        properties.load(input);
        allNodes = Integer.parseInt(properties.getProperty("number_of_nodes"));

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
        Service rrpcTotal = new AppiaService("rrpc_total");
        Service rrpc = new AppiaService("rrpc");
        this.init(control, session, rrpc, rrpcTotal, logId);
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
            System.out.println("I am leaving the group");
            //e.printStackTrace();
        }
    }

    public void write(Tuple tuple) {
        System.out.println("all and current : " + allNodes + " , " + membersInGroup);
        if (membersInGroup > Math.ceil(allNodes / 2)) {

            if (isBlocked)
                System.out.println("........... operations are blocked until new view is delivered. Waiting until unblocked to write.......");

            //Avoid sending messages after Block OK is issued
            while (isBlocked) {}

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
        System.out.println("all and current : " + allNodes + " , " + membersInGroup);
        if (membersInGroup > Math.ceil(allNodes / 2)) {

            if (isBlocked)
                System.out.println("........... operations are blocked until new view is delivered. Waiting until unblocked to take.......");

            //Avoid sending messages after Block OK is issued
            while (isBlocked) {}

            while(tupleManager.getMatchingTuples(template).size() == 0) {}
            TupleMessage msg = new TupleMessage(template, Type.TAKE);
            sendClientRequest(msg);

        } else {
            System.out.println("You are in a minority partition. Cannot execute write request.");
        }
    }

    public void isolate() {
        System.out.println("isolate received at Server");
        isIsolated = true;
        membersInGroup = 1;
    }

    public void recover() {
        System.out.println("recover received at Server");
        isIsolated = false;
    }

    public void printStatus() {
        System.out.println("\n \n #################### Tuple Status ##################");
        TupleSpace tupleSpace = tupleManager.getTupleSpace();
        CopyOnWriteArrayList<Tuple> tuples = tupleSpace.getTuples();
        for (Tuple tuple : tuples) {
            String[] values = tuple.getValues();
            System.out.println(values[0] + ", " + values[1] + ", " + values[2]);
        }
        System.out.println("###############################################");

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
            System.out.println("====== sending client request : BCAST......" + ((MembershipSession) control).getMembership().getMembershipList().size());
            if(Type.TAKE.equals(tupleMsg.getType().name()))
                groupSession.send(msg, totalGroup, null, null);
            else
                groupSession.send(msg, group, null, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendResultsNotificationToClient(Tuple tuple, Type type) throws IOException {
        getClient().receiveResults(tuple, type);
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

        try {
            System.out.println("-- NEW VIEW: " + ((MembershipSession) control).getMembership().getMembershipID() +
                    "\tSize: " + ((MembershipSession) control).getMembership().getMembershipList().size());

            int noOfMembers = ((MembershipSession) control).getMembership().getMembershipList().size();
            Membership membership = ((MembershipSession) control).getMembership();
            String viewIdString = membership.getMembershipID().toString().split(";")[0].split(":")[1];
            int localId = ((MembershipSession) control).getMembership().getLocalRank();
            localAddress = ((MembershipSession) control).getMembership().getMemberAddress(localId);

            int newViewId = 0;

            if (noOfMembers > Math.ceil(allNodes / 2) && viewIdString != null) { // sync view only if you are in the majority view
                newViewId = Integer.valueOf(viewIdString);
                try {
                    System.out.println("current view Id : " + StateManager.getInstance().getCurrentViewId());
                    System.out.println("new view Id : " + newViewId);
                    StateManager.getInstance().sync(((MembershipSession) control).getMembership().getMembershipList(), newViewId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(newViewId != 0)
                    StateManager.getInstance().updateLastMajorityViewId(newViewId);
            }

            membersInGroup = ((MembershipSession) control).getMembership().getMembershipList().size();
            System.out.println("membership changed.......");
            isBlocked = false;
        } catch (NotJoinedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // this notification is issued before a new view
    // a new view will not appear while the flush is not notified
    // (using the blockOk() method). After this, no message can be sent
    // while waiting for a new view.
    public void onBlock() {
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
                System.out.println("_____ TupleMessage received ____");
                handleRquest((TupleMessage) protoMsg, msg.getSenderAddress());
                return null;

            } else if (protoMsg instanceof LogRequestMessage) {

                System.out.println("____ LogRequestMessage received ____");
                try {
                    StateManager.getInstance().sendLogsToMerge((LogRequestMessage) protoMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (protoMsg instanceof LogResponseMessage) {
                System.out.println("____ LogResponseMessage received ____");
                try {
                    protoMsg.unmarshal();
                    StateManager.getInstance().addToBlockingQueue((LogResponseMessage) protoMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else if(protoMsg instanceof TakeResponseMessage) {
                System.out.println("____ take response received ____");
                try {
                    protoMsg.unmarshal();
                    tupleManager.addTakeResponse((TakeResponseMessage) protoMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (protoMsg instanceof ServerMessage) {
                System.out.println("____ server msg received ____");
                /*handleServerMessage((ServerMessage) protoMsg,
                        msg.getSenderAddress());*/
                return null;
            }
            return null;
        }

        public void onServiceEnsured(Object context, Service service) {
        }

        private void handleRquest(TupleMessage tupleMessage, SocketAddress addr) {
            try {
                tupleMessage.unmarshal();

                SocketAddress myAddress;
                if(localAddress != null) {
                     myAddress = localAddress;
                } else {
                    myAddress = control.getLocalAddress();
                }
                if (addr.equals(myAddress)) {
//                    System.out.println("\tReceived request message. I'm the origin)");
//                    System.out.println("tuple msg type in request : " + tupleMessage.getType());
                } else {
//                    System.out.println("\tReceived request message");
                }
                System.out.println("Type: " + tupleMessage.getType());

                String[] tupleValues = tupleMessage.getTuple().getValues();
                switch (tupleMessage.getType()) {
                    case WRITE:
                        tupleManager.writeTuple(tupleMessage.getTuple());
                        logManager.writeLog(tupleMessage, StateManager.getInstance().getCurrentViewId());
                        break;
                    case TAKE:
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

    public TupleSpace getTupleSpace(){
       return tupleManager.getTupleSpace();
    }

}
