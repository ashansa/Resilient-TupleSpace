package org.ist.rsts;


import net.sf.jgcs.DataSession;
import net.sf.jgcs.Message;
import net.sf.jgcs.Service;
import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.TupleManager;
import org.ist.rsts.tuple.Type;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
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
            System.out.println("TODO.............. STATE TRANSFER.....");
            /*requestLogs(memberList.get(new Random().nextInt(memberList.size())));
            mergeLogs();*/
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
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, logRequestMsg.getByteArray());
        msg.setPayload(bytes);

        groupSession.send(msg, group, logRequestMessage.getSenderAddress(), null, null);
    }

    private void requestLogs(SocketAddress destination) throws IOException {
        System.out.println("view id ======" + viewId);
        LogRequestMessage logRequestMsg = new LogRequestMessage(Integer.valueOf(viewId));
        Message msg = groupSession.createMessage();
        logRequestMsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, logRequestMsg.getByteArray());
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
        HashMap<Integer, String> logMap = responseMessage.getLogs();
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

    private void updateTuples(String logLine) {
        //log line format
        //seqNo:WRITE/TAKE;value1,value2,value3
        System.out.println("log line >>>>>>> " + logLine);
        try {
            String operation = logLine.split(":")[1].split(";")[0];
            String[] values = logLine.split(";")[1].split(",");
            Tuple tuple = new Tuple(values[0], values[1], values[2]);

            if (Type.WRITE.name().equals(operation)) {
                System.out.println("...........WRITE update........");
                tupleManager.writeTuple(tuple);
            } else if (Type.TAKE.name().equals(operation)) {
                System.out.println("...........TAKE update...........");
                tupleManager.takeTuple(tuple);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not update tuple for log line: " + logLine);
        }
    }

    private HashMap<Integer, String> getLogs(int requesterViewId) throws IOException {
        HashMap<Integer, String> logs = new HashMap<Integer, String>();

        // travesing from previous view to current view.
        for (int i = requesterViewId - 1; i < getCurrentViewId() + 1; i++) {
            logs.put(i, LogManager.getLogForView(i));
        }
        return logs;
    }

    public void addToBlockingQueue(LogResponseMessage response) {
        blockingQueue.add(response);
    }

}
