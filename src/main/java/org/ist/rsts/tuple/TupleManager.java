package org.ist.rsts.tuple;

import org.ist.rsts.ServerGroup;

import java.io.IOException;
import java.util.Vector;

public class TupleManager {

    private TupleSpace tupleSpace= new TupleSpace();
    //TODO: can we have > 1 pending req? since we block when there is a pending req
    Vector<Tuple> pendingReadRequests = new Vector<Tuple>();
    Vector<Tuple> pendingTakeRequests = new Vector<Tuple>();
    Vector<Tuple> pendingTakeDecisions = new Vector<Tuple>();
    ServerGroup server;

    public TupleManager(ServerGroup server) {
        this.server = server;
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
        Vector<Tuple> results = tupleSpace.read(template);
        if(results.size() > 0) {
            return results.firstElement();
        }
        else {
            pendingReadRequests.add(template);
            return null;
        }
    }

    public Tuple getTupleForTake(Tuple template, boolean isRetry) {
        Vector<Tuple> matches = tupleSpace.read(template);
        if (matches.size() > 0) {
            //TODO: Select one common match to delete
            Tuple toTake = matches.firstElement();
            return toTake;
        } else {
            if (!isRetry) {
                pendingTakeDecisions.add(template);
            }
            return null;
        }
    }

    public Tuple takeTuple(Tuple tuple) {
        Vector<Tuple> matches = tupleSpace.read(tuple);
        if(matches.size() > 0) {
            //TODO: Select one common match to delete
            Tuple toTake = matches.firstElement();
            tupleSpace.remove(toTake);
            return toTake;
        } else {
            pendingTakeRequests.add(tuple);
            return null;
        }
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



    private void servePendingReadRequests(Tuple newTuple) {
        for (int i = 0; i < pendingReadRequests.size(); i++) {
            boolean match = isAMatch(pendingReadRequests.get(i), newTuple);
            if(match) {
                System.out.println("found a match for a pending read.....");
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
}
