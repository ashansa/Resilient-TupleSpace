package org.ist.rsts.tuple;

import org.ist.rsts.ServerGroup;

import java.io.IOException;
import java.util.Vector;

public class TupleManager {

    private TupleSpace tupleSpace= new TupleSpace();
    //TODO: can we have > 1 pending req? since we block when there is a pending req
    Vector<Tuple> pendingReadRequests = new Vector<Tuple>();
    Vector<Tuple> pendingTakeRequests = new Vector<Tuple>();
    ServerGroup server;

    public TupleManager(ServerGroup server) {
        this.server = server;
    }

    public void writeTuple(Tuple tuple) {
        tupleSpace.write(tuple);
        if(pendingReadRequests.size() > 0)
            servePendingReadRequests(tuple);
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

    public Tuple takeTuple(Tuple template) {
        Tuple result = tupleSpace.take(template);
        if(result != null) {
            return result;
        }
        else {
            pendingTakeRequests.add(template);
            return null;
        }
    }


    private void servePendingReadRequests(Tuple newTuple) {
        for (int i = 0; i < pendingReadRequests.size(); i++) {
            boolean match = isAMatch(pendingReadRequests.get(i), newTuple);
            if(match) {
                System.out.println("found a match for a pending.....");
                try {
                    server.sendResultsNotificationToClient(newTuple, Type.READ);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
