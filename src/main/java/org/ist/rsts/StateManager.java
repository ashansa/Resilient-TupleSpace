package org.ist.rsts;


import net.sf.jgcs.DataSession;
import net.sf.jgcs.Message;
import net.sf.jgcs.Service;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This will store and manage all the relevant information regarding State Machine replication for a server
 */
public class StateManager {

    private static StateManager stateManager;
    private String viewId;
    private DataSession groupSession;
    private Service group;
    private Hashtable logRequests;
    private ReentrantLock lock;

    protected StateManager() {
    }

    public void init(DataSession groupSession, Service group) {
        this.groupSession = groupSession;
        this.group = group;
        lock = new ReentrantLock();
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

    public void setViewNumber(String viewId) {
        this.viewId = viewId;
    }

    public String getCurrentViewId() {
        return viewId;
    }

    public void syncStates(List<SocketAddress> memberList) throws IOException {
        requestLogs(memberList.get(new Random().nextInt(memberList.size())));
    }

    private void requestLogs(SocketAddress destination) throws IOException {
        LogRequestMessage logRequestMsg = new LogRequestMessage(Integer.valueOf(viewId));
        Message msg = groupSession.createMessage();
        logRequestMsg.marshal();
        byte[] bytes = Constants.createMessageToSend(Constants.MessageType.TUPLE, logRequestMsg.getByteArray());
        msg.setPayload(bytes);

        groupSession.send(msg, group, destination, null, null);
    }


}
