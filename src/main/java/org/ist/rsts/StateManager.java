package org.ist.rsts;

import net.sf.jgcs.DataSession;
import net.sf.jgcs.Message;
import net.sf.jgcs.Service;
import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.TupleManager;
import org.ist.rsts.tuple.Type;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * This will store and manage all the relevant information regarding State Machine replication for a server
 */
public class StateManager {

    private static StateManager stateManager;
    private int lastMajorityViewId = 0;
    private DataSession groupSession;
    private Service group;
    private Hashtable logRequests;
    private ArrayBlockingQueue<LogResponseMessage> blockingQueue;
    private TupleManager tupleManager;
    private final static Logger logger = Logger.getLogger(StateManager.class.getName());
    private ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
    private LogManager logManager;
    private ServerGroup server;

    protected StateManager() {
    }

    public void init(DataSession groupSession, Service group, TupleManager tupleManager, LogManager logManager, ServerGroup server) {
        this.groupSession = groupSession;
        this.group = group;
        blockingQueue = new ArrayBlockingQueue(1, true);
        this.tupleManager = tupleManager;
        this.logManager = logManager;
        this.server = server;
    }

    /**
     * Returns instance of the StateManger
     *
     * @return
     */
    public static StateManager getInstance() {
        if (stateManager == null) {
            stateManager = new StateManager();
        }
        return stateManager;
    }

    public void updateLastMajorityViewId(int viewId) {
        this.lastMajorityViewId = viewId;
    }

    public int getCurrentViewId() {
        return lastMajorityViewId;
    }

    public void sync(List<SocketAddress> memberList, int newId){
        System.out.println("OLD...., NEW... : " + lastMajorityViewId + "," + newId);
        if (lastMajorityViewId != newId - 1 && memberList.size()>1) {
            singleExecutor.execute(new SyncTask(memberList, lastMajorityViewId, newId));

        } else {//I have not been in the last view. Need to transfer state
            System.out.println("........... NO STATE TRANSFER needed......");
        }

    }

    public void sendLogsToMerge(LogRequestMessage logRequestMessage) throws IOException {
        try {
            logRequestMessage.unmarshal();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        int requesterViewId = logRequestMessage.getViewId();
        String sender =  logRequestMessage.getSenderAddressString().toString();
        // Need to send correct log messages.

        LogResponseMessage logRequestMsg = new LogResponseMessage(getLogs(requesterViewId));
        Message msg = groupSession.createMessage();
        logRequestMsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.LOG_RESPONSE, logRequestMsg.getByteArray());
        msg.setPayload(bytes);
        System.out.println("....... going to send log response to :" + sender);
        SocketAddress dest = new InetSocketAddress(sender.split(":")[0],Integer.valueOf(sender.split(":")[1]));
        System.out.println(dest);
        groupSession.send(msg, group, null, dest, null);
    }

    private void requestLogs(SocketAddress destination, int lastPresentViewId) throws IOException {
        SocketAddress mySocketAddress = server.getLocalAddress();
        System.out.println(mySocketAddress.toString().replace("/", ""));
        LogRequestMessage logRequestMsg = new LogRequestMessage(lastPresentViewId, new StringBuffer(mySocketAddress.toString().replace("/", "")));
        logRequestMsg.setSenderAddress(mySocketAddress);
        Message msg = groupSession.createMessage();
        logRequestMsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.LOG_REQUEST, logRequestMsg.getByteArray());
        msg.setPayload(bytes);
        System.out.println("Requesting logs from ====> "+destination);
        groupSession.send(msg, group, null,destination, null);
    }

    private void mergeLogs(int lastPresentViewId) throws InterruptedException {
        System.out.println("Staring log merging and waiting on log response");
        LogResponseMessage responseMessage = blockingQueue.take();

        String myLastViewLog = null;
        try {
            myLastViewLog = logManager.getLogForView(lastPresentViewId);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("========== UNDO MY LAST LOG FAILED. Couldn't get last log ======");
        }
        if(myLastViewLog != null) {
            //undo operations that he has done in his last view
            undoOperation(myLastViewLog);
            logManager.clearLogFile(lastPresentViewId);
        }

        //apply operations taken received
        System.out.println("__________ received log from another__________ " + responseMessage);
        HashMap<Integer, String> logMap = responseMessage.getLogs();
        for (Integer viewId : logMap.keySet()) {
            System.out.println("Id and log : " + viewId + ", " + logMap.get(viewId));
        }

        for (Map.Entry<Integer, String> entry : logMap.entrySet()) {
            int viewId = entry.getKey();
            String logString = entry.getValue();
            String[] logLines = logString.split("\n");
            for (String log : logLines) {
                //write:a,b,c
                if(log.isEmpty()){
                    continue;
                }
                String operation = log.split(":")[0];
                String[] values = log.split(":")[1].split(",");
                Tuple tuple = new Tuple(values[0], values[1], values[2]);

                //update tuple space
                if (Type.WRITE.name().equals(operation)) {
                    tupleManager.writeTuple(tuple);
                } else if (Type.TAKE.name().equals(operation)) {
                    tupleManager.takeTuple(tuple);
                }

                //write to log
                logManager.writeLog(tuple, operation, viewId);
            }
        }
    }

    private void undoOperation(String logString) {
        String[] logLines = logString.split("\n");
        for (String log : logLines) {
            //write:a,b,c
            if(log.isEmpty()){
                continue;
            }
            String operation = log.split(":")[0];
            String[] values = log.split(":")[1].split(",");
            Tuple tuple = new Tuple(values[0], values[1], values[2]);

            //update tuple space
            if (Type.WRITE.name().equals(operation)) {
                tupleManager.takeTuple(tuple);
            } else if (Type.TAKE.name().equals(operation)) {
                tupleManager.writeTuple(tuple);
            }
        }
    }

    private HashMap<Integer, String> getLogs(int requesterViewId) throws IOException {
        HashMap<Integer, String> logs = new HashMap<Integer, String>();

        System.out.println("....... Requested Log View..... " + requesterViewId);
        System.out.println("....... current View ID ..... " + getCurrentViewId());
        // travesing from previous view to current view.
        for (int i = requesterViewId; i < getCurrentViewId() + 1; i++) {
            logs.put(i, logManager.getLogForView(i));
        }
        return logs;
    }

    public void addToBlockingQueue(LogResponseMessage response) {
        try {
            blockingQueue.put(response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class SyncTask implements Runnable{
        List<SocketAddress> memberList;
        int lastPresentViewId;
        int newId;
       public SyncTask(List<SocketAddress> memberList, int lastPresentViewId, int newId){
           this.memberList = memberList;
           this.lastPresentViewId = lastPresentViewId;
           this.newId = newId;
       }

        @Override
        public void run() {
            ArrayList<SocketAddress> otherMembers = new ArrayList<SocketAddress>();
            for (SocketAddress member : memberList) {
                if(!member.equals(server.getLocalAddress()))
                    otherMembers.add(member);
            }
            try {
                SocketAddress receiver = otherMembers.get(new Random().nextInt(otherMembers.size()));
                System.out.println("TODO.............. STATE TRANSFER....." + receiver.toString());
                requestLogs(receiver, lastPresentViewId);
                System.out.println("Logs requested");
                mergeLogs(lastPresentViewId);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
