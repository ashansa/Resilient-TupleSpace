package org.ist.rsts.tuple;

import org.ist.rsts.LogManager;
import org.ist.rsts.ServerGroup;
import org.ist.rsts.StateManager;
import org.ist.rsts.TakeResponseMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class TupleManager {

    private TupleSpace tupleSpace= new TupleSpace();
    Vector<Tuple> pendingReadRequests = new Vector<Tuple>();
    Vector<Tuple> pendingTakeRequests = new Vector<Tuple>();
    public HashMap<UUID, Vector<TakeResponseMessage>> takeResponses = new HashMap<UUID, Vector<TakeResponseMessage>>();
    private ArrayBlockingQueue<TupleMessage> takeTemplatesQueue = new ArrayBlockingQueue<TupleMessage>(1000);

    ServerGroup server;
    LogManager logManager;

    public TupleManager(ServerGroup server, LogManager logManager) {
        this.server = server;
        this.logManager = logManager;
        new Thread(new TakeConsumer()).start();
    }

    public void writeTuple(Tuple tuple) {
        tupleSpace.write(tuple);
        if(pendingReadRequests.size() > 0)
            servePendingReadRequests(tuple);
        if(pendingTakeRequests.size() > 0)
            servePendingTakeRequests(tuple);
    }

    public Tuple readTuple(Tuple template) {
        Vector<Tuple> results = tupleSpace.getMatchingTuples(template);
        if(results.size() > 0) {
            return results.firstElement();
        }
        else {
            pendingReadRequests.add(template);
            return null;
        }
    }

    public Tuple takeTuple(Tuple tuple) {
        //need to check whether the tuple write has been received to this node before removing
        Vector<Tuple> matches = tupleSpace.getMatchingTuples(tuple);
        if(matches.size() > 0) {
            Tuple toTake = matches.firstElement();
            tupleSpace.remove(toTake);

            try {
                server.sendResultsNotificationToClient(toTake, Type.TAKE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return toTake;
        } else {
            System.out.println("-------- no matches found. write not received yet. add to pending take.......");
            pendingTakeRequests.add(tuple);
            return null;
        }
    }

    public void processTakeOperation(Vector<TakeResponseMessage> takeResponseMessages) {
        Tuple tupleToTake = decideTupleToTake(takeResponseMessages);
        if(tupleToTake != null) {
            System.out.println("......DECIDED TUPLE TO TAKE :");
            for (String s : tupleToTake.getValues()) {
                System.out.println(s);
            }
            Tuple tupleTaken = takeTuple(tupleToTake);
            logManager.writeLog(tupleTaken, Type.TAKE.name(), StateManager.getInstance().getCurrentViewId());
        }
    }

    private Tuple decideTupleToTake(Vector<TakeResponseMessage> takeResponseMessages) {
        Vector<Tuple> allMatchingTuples = new Vector<Tuple>();
        for (TakeResponseMessage response : takeResponseMessages) {
            allMatchingTuples.addAll(response.getMatchingTuples());
        }
        if(allMatchingTuples.size() > 0) {
            Collections.sort(allMatchingTuples);
            return allMatchingTuples.get(0);
        } else {
            //no one has a matching tuple for the take
            return null;
        }

    }

    public void addTakeResponse(TakeResponseMessage responseMessage) {
        UUID takeUUID = responseMessage.getUuidOfTake();
        Vector<TakeResponseMessage> receivedResponses = takeResponses.get(takeUUID);
        if(receivedResponses == null) {
            receivedResponses = new Vector<TakeResponseMessage>();
            receivedResponses.add(responseMessage);
        } else {
            receivedResponses.add(responseMessage);
        }
        takeResponses.put(takeUUID, receivedResponses);
    }

    public Vector<Tuple> getMatchingTuples(Tuple template) {
        return tupleSpace.getMatchingTuples(template);
    }

    public void addToTakeQueue(TupleMessage msg) throws InterruptedException {
        System.out.println("........ adding take template to queue : " +msg.getUUID() + " , " + msg.getTuple().getValues());
        takeTemplatesQueue.put(msg);
    }

    private void servePendingReadRequests(Tuple newTuple) {
        for (int i = 0; i < pendingReadRequests.size(); i++) {
            boolean match = isAMatch(pendingReadRequests.get(i), newTuple);
            if(match) {
                try {
                    server.sendResultsNotificationToClient(newTuple, Type.READ);
                    pendingReadRequests.remove(pendingReadRequests.get(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void servePendingTakeRequests(Tuple newTuple) {
        for (int i = 0; i < pendingTakeRequests.size(); i++) {
            try {
                boolean match = isAMatch(pendingTakeRequests.get(i), newTuple);
                if (match) {
                    Tuple tupleTaken = takeTuple(pendingTakeRequests.get(i));
                    if (tupleTaken != null) {
                        logManager.writeLog(tupleTaken, Type.TAKE.name(), StateManager.getInstance().getCurrentViewId());
                        server.sendResultsNotificationToClient(tupleTaken, Type.TAKE);
                        pendingTakeRequests.remove(pendingTakeRequests.get(i));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isAMatch(Tuple template, Tuple tuple) {
        String[] tupleValues = tuple.getValues();
        String[] templateElements = template.getValues();
        //checking 1st value
        if (("*".equals(templateElements[0]) || tupleValues[0].equals(templateElements[0])) &&
                ("*".equals(templateElements[1]) || tupleValues[1].equals(templateElements[1])) &&
                ("*".equals(templateElements[2]) || tupleValues[2].equals(templateElements[2]))) {
            return true;
        }

        return false;
    }

    private class TakeConsumer implements Runnable{

        @Override
        public void run() {
            while (true){
                try {
                    TupleMessage takeMsg = takeTemplatesQueue.take();
                    System.out.println("....... ONE TAKE MESSAGE TAKEN FROM QUEUE..........");
                    UUID uuidOfTake = takeMsg.getUUID();
                    Vector<Tuple> matchingTuples = getMatchingTuples(takeMsg.getTuple());
                    server.bcastMatchingTuplesForTake(matchingTuples, uuidOfTake);
                    while (!isResponsesComplete(uuidOfTake)) {
                        //do noting
                    }
                    System.out.println("............ all responses received for take .......");
                    Vector<TakeResponseMessage> responses = takeResponses.get(uuidOfTake);
                    processTakeOperation(responses);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean isResponsesComplete(UUID uuid) {
            Vector<TakeResponseMessage> responses = takeResponses.get(uuid);
            if(responses != null && responses.size() == server.getMembersInGroup())
                return true;
            else
                return false;
        }
    }

    //this will be used for tests to show status
    public TupleSpace getTupleSpace(){
      return tupleSpace;
    }
}
