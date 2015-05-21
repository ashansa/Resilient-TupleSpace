package org.ist.rsts;


import net.sf.jgcs.DataSession;
import net.sf.jgcs.Message;
import net.sf.jgcs.Service;
import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.TupleManager;
import org.ist.rsts.tuple.Type;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
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
    private LogManager logManager;

    protected StateManager() {
    }

    public void init(DataSession groupSession, Service group, TupleManager tupleManager, LogManager logManager) {
        this.groupSession = groupSession;
        this.group = group;
        blockingQueue = new ArrayBlockingQueue(1, true);
        this.tupleManager = tupleManager;
        this.logManager = logManager;
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

    public void syncStates(List<SocketAddress> memberList, int newId) throws IOException, InterruptedException {
        System.out.println("OLD...., NEW... : " + newId + "," + newId);
        if (viewId != newId - 1) { //I have not been in the last view. Need to transfer state
            SocketAddress receiver = memberList.get(new Random().nextInt(memberList.size()));
            System.out.println("TODO.............. STATE TRANSFER....." + receiver.toString());

            requestLogs(receiver);
            mergeLogs();
        } else {
            System.out.println("........... NO STATE TRANSFER needed......");
        }
    }

    public void sendLogsToMerge(LogRequestMessage logRequestMessage) throws IOException {
        int requesterViewId = logRequestMessage.getViewId();
        // Need to send correct log messages.

        LogResponseMessage logRequestMsg = new LogResponseMessage(getLogs(requesterViewId));
        Message msg = groupSession.createMessage();
        logRequestMsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.LOG_RESPONSE, logRequestMsg.getByteArray());
        msg.setPayload(bytes);

        System.out.println("....... going to send log response to :" + logRequestMessage.getSenderAddress());
        groupSession.send(msg, group, logRequestMessage.getSenderAddress(), null, null);
    }

    private void requestLogs(SocketAddress destination) throws IOException {
        System.out.println("view id ======" + viewId);
        LogRequestMessage logRequestMsg = new LogRequestMessage(viewId);
        Message msg = groupSession.createMessage();
        logRequestMsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.LOG_REQUEST, logRequestMsg.getByteArray());
        msg.setPayload(bytes);

        groupSession.send(msg, group, destination, null, null);
    }

    private void mergeLogs() throws InterruptedException {
        LogResponseMessage responseMessage = blockingQueue.take();
       /* String log = responseMessage.getLog();
        String[] logs = log.split("\n");
        for (String s : logs) {
            updateTuples(s.trim());
        }
*/

        String myLastViewLog = null;
        try {
            myLastViewLog = logManager.getLogForView(viewId);
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
            logs.put(i, LogManager.getLogForView(i));
        }
        return logs;
    }

    public void addToBlockingQueue(LogResponseMessage response) {
        blockingQueue.add(response);
    }

}
