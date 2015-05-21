package org.ist.rsts.tuple;

import org.ist.rsts.ServerGroup;
import org.ist.rsts.TakeResponseMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class TupleManager {

    private TupleSpace tupleSpace= new TupleSpace();
    //TODO: can we have > 1 pending req? since we block when there is a pending req
    Vector<Tuple> pendingReadRequests = new Vector<Tuple>();
    Vector<Tuple> pendingTakeRequests = new Vector<Tuple>();
    Vector<Tuple> pendingTakeDecisions = new Vector<Tuple>();
    ServerGroup server;
    public HashMap<UUID, Vector<TakeResponseMessage>> takeResponses = new HashMap<UUID, Vector<TakeResponseMessage>>();

    public TupleManager(ServerGroup server) {
        this.server = server;
        new Thread(new TakeConsumer()).start();
    }

    public void writeTuple(Tuple tuple) {
        tupleSpace.write(tuple);
        if(pendingReadRequests.size() > 0)
            servePendingReadRequests(tuple);
        if(pendingTakeRequests.size() > 0)
            servePendingTakeRequests(tuple);
        if(pendingTakeDecisions.size() > 0)
            servePendingTakeDecisions(tuple);
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

    public Tuple getTupleForTake(Tuple template, boolean isRetry) {
        Vector<Tuple> matches = tupleSpace.getMatchingTuples(template);
        if (matches.size() > 0) {
            //select a random value to lower the chance of two clients selecting same concurrently
            Tuple toTake = matches.get(new Random().nextInt(matches.size()-1));
            return toTake;
        } else {
            if (!isRetry) {
                pendingTakeDecisions.add(template);
            }
            return null;
        }
    }

    public Tuple takeTuple(Tuple tuple) {
        //need to check whether the tuple write has been received to this node before removing
        Vector<Tuple> matches = tupleSpace.getMatchingTuples(tuple);
        if(matches.size() > 0) {
            Tuple toTake = matches.firstElement();
            tupleSpace.remove(toTake);
            return toTake;
        } else {
            pendingTakeRequests.add(tuple);
            return null;
        }
    }

    public void processTakeOperation(Vector<TakeResponseMessage> takeResponseMessages) {
        Tuple tupleToTake = decideTupleToTake(takeResponseMessages);
        takeTuple(tupleToTake);
    }

    private Tuple decideTupleToTake(Vector<TakeResponseMessage> takeResponseMessages) {
        Tuple tupleToTake = null;
        Vector<Tuple> allMatchingTuples = new Vector<Tuple>();
        for (TakeResponseMessage response : takeResponseMessages) {
            allMatchingTuples.addAll(response.getMatchingTuples());
        }
        //TODO............ do a sort and get the answer
        return allMatchingTuples.get(0);
    }

    public void addTakeResponse(TakeResponseMessage responseMessage) {
        UUID takeUUID = responseMessage.getUuidOfTake();
        Vector<TakeResponseMessage> receivedResponses = takeResponses.get(takeUUID);
        System.out.println(".......... already received responses for UUID >>>> " + takeUUID + " : " + receivedResponses);
        if(receivedResponses == null) {
            receivedResponses = new Vector<TakeResponseMessage>();
            receivedResponses.add(responseMessage);
        } else {
            receivedResponses.add(responseMessage);
        }
        System.out.println("........ now all collected responses for UUID >>>> " + takeUUID + " : " + receivedResponses.size());
        takeResponses.put(takeUUID, receivedResponses);
    }

    private void servePendingTakeDecisions(Tuple newTuple) {
        //this new tuple may have been removed due to a pending take request. So check exist too
        for (int i = 0; i < pendingTakeDecisions.size(); i++) {
            boolean match = isAMatch(pendingTakeDecisions.get(i), newTuple);
            if (match) {
                Tuple tupleToTake = getTupleForTake(pendingTakeDecisions.get(i), true);
                if (tupleToTake != null) {
                    System.out.println("found a match for a pending take DECISION. notifying client");
                    server.receiveTakeDecisionResult(tupleToTake);
                    pendingTakeDecisions.remove(pendingTakeDecisions.get(i));
                }
            }
        }
    }

    public Vector<Tuple> getMatchingTuples(Tuple template) {
        return tupleSpace.getMatchingTuples(template);
    }

    private ArrayBlockingQueue<TupleMessage> takeTemplatesQueue = new ArrayBlockingQueue<TupleMessage>(1000);

    public void addToTakeQueue(TupleMessage msg) {
        System.out.println("........ adding tuple msg to queue : " +msg.getUUID() + " , " + msg.getTuple().getValues());
        takeTemplatesQueue.add(msg);
    }

    private void servePendingReadRequests(Tuple newTuple) {
        for (int i = 0; i < pendingReadRequests.size(); i++) {
            boolean match = isAMatch(pendingReadRequests.get(i), newTuple);
            if(match) {
                System.out.println("found a match for a pending getMatchingTuples.....");
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
                    System.out.println("found a match for a pending take. Try take again....");
                    Tuple tupleTaken = takeTuple(pendingTakeRequests.get(i));
                    if (tupleTaken != null) {
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
            System.out.println("+++++++++++++++++ TAKE CONSUMER STARTED ++++++++++++++");
            while (true){
                try {
                    TupleMessage takeMsg = takeTemplatesQueue.take();
                    System.out.println("....... ONE TAKE MESSAGE TAKEN FROM QUEUE..........");
                    UUID uuidOfTake = takeMsg.getUUID();
                    Vector<Tuple> matchingTuples = getMatchingTuples(takeMsg.getTuple());
                    server.bcastMatchingTuplesForTake(matchingTuples, uuidOfTake);
                    while (!isResponsesComplete(uuidOfTake)) {
                        //do nothing
                        //temp sleep
                        Thread.sleep(2000);
                    }
                    Vector<TakeResponseMessage> responses = takeResponses.get(uuidOfTake);
                    processTakeOperation(responses);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean isResponsesComplete(UUID uuid) {
            Vector<TakeResponseMessage> responses = takeResponses.get(uuid);
            System.out.println("responses, members in grp : " +responses + " , " + server.getMembersInGroup());
            if(responses != null)
                System.out.println("no of responses : " + responses.size());


            if(responses != null && responses.size() == server.getMembersInGroup())
                return true;
            else
                return false;
        }
    }
}
