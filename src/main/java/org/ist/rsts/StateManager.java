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
    private int viewId = -1;
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

    public void setViewNumber(int viewId) {
        this.viewId = viewId;
    }

    public int getCurrentViewId() {
        return viewId;
    }

    public void sync(List<SocketAddress> memberList, int newId){
        System.out.println("OLD...., NEW... : " + viewId + "," + newId);
        if (viewId != newId - 1 && memberList.size()>1) { //I have not been in the last view. Need to transfer state

            singleExecutor.execute(new SyncTask(memberList,viewId, newId));

        } else {
            System.out.println("........... NO STATE TRANSFER needed......");
        }

    }

    /*public void syncStates(List<SocketAddress> memberList, int newId) throws IOException, InterruptedException {

        //removing my id from list
//        if(memberList.contains(server.getLocalAddress())) {
//            memberList.remove(server.getLocalAddress());
//        }
        System.out.println("OLD...., NEW... : " + viewId + "," + newId);
        if (viewId != newId - 1 && memberList.size()>1) { //I have not been in the last view. Need to transfer state
            SocketAddress receiver = memberList.get(new Random().nextInt(memberList.size()));
            System.out.println("TODO.............. STATE TRANSFER....." + receiver.toString());
            requestLogs(receiver);
            System.out.println("Logs are requested");
            mergeLogs();
        } else {
            System.out.println("........... NO STATE TRANSFER needed......");
        }
    }*/

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
        System.out.println("vieeew "+requesterViewId);
        System.out.println("....... going to send log response to :" + sender);
        SocketAddress dest = new InetSocketAddress(sender.split(":")[0],Integer.valueOf(sender.split(":")[1]));
        System.out.println(dest);
        groupSession.send(msg, group, null, dest, null);
    }

    private void requestLogs(SocketAddress destination, int lastPresentViewId) throws IOException {
        SocketAddress mySocketAddress = server.getLocalAddress();
        System.out.println("@@@@@@@@@ last present view id, local address ======" + lastPresentViewId+","+mySocketAddress);
        System.out.println(mySocketAddress.toString().replace("/", ""));
        LogRequestMessage logRequestMsg = new LogRequestMessage(lastPresentViewId, new StringBuffer(mySocketAddress.toString().replace("/", "")));
        logRequestMsg.setSenderAddress(mySocketAddress);
        Message msg = groupSession.createMessage();
        logRequestMsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.LOG_REQUEST, logRequestMsg.getByteArray());
        msg.setPayload(bytes);
        System.out.println("Sending to====> "+destination);
        groupSession.send(msg, group, null,destination, null);
    }

    private void mergeLogs(int lastPresentViewId) throws InterruptedException {
        System.out.println("Staring log merging and waiting on take");
        LogResponseMessage responseMessage = blockingQueue.take();
       /* String log = responseMessage.getLog();
        String[] logs = log.split("\n");
        for (String s : logs) {
            updateTuples(s.trim());
        }
*/

        String myLastViewLog = null;
        try {
            myLastViewLog = logManager.getLogForView(lastPresentViewId);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("========== UNDO MY LAST LOG FAILED. Couldn't get last log ======");
        }
        if(myLastViewLog != null) {
            //undo operations that he has done in his last view
            //TODO: do this....
        }

        //apply operations taken received
        System.out.println("__________ received log from another__________ " + responseMessage);
        HashMap<Integer, String> logMap = responseMessage.getLogs();
        System.out.println("___ log files ____" + logMap.size());
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
            try {
                if(memberList.contains(server.getLocalAddress())) {
                    memberList.remove(server.getLocalAddress());
                }
                SocketAddress receiver = memberList.get(new Random().nextInt(memberList.size()));
                System.out.println("TODO.............. STATE TRANSFER....." + receiver.toString());
                requestLogs(receiver, lastPresentViewId);
                System.out.println("Logs are requested");
                mergeLogs(lastPresentViewId);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
