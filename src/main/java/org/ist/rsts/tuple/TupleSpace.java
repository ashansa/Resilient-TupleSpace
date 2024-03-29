package org.ist.rsts.tuple;

import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class TupleSpace {

    CopyOnWriteArrayList<Tuple> tuples = new CopyOnWriteArrayList<Tuple>();

    public boolean write(Tuple tuple) {
        tuples.add(tuple);
        return true;
    }

    public Vector<Tuple> getMatchingTuples(Tuple template) {
        Vector<Tuple> matches = new Vector<Tuple>();
        String[] templateElements = template.getValues();

        for (Tuple tuple : tuples) {
            String[] tupleValues = tuple.getValues();
            //checking 1st value
            if (!("*".equals(templateElements[0]) || tupleValues[0].equals(templateElements[0]))) {
                continue;
            }
            //checking 2nd value
            if (!("*".equals(templateElements[1]) || tupleValues[1].equals(templateElements[1]))) {
                continue;
            }
            //checking 3rd value
            if (!("*".equals(templateElements[2]) || tupleValues[2].equals(templateElements[2]))) {
                continue;
            }
            matches.add(tuple);
        }
        //add matches to this
        return matches;
    }

    public void remove(Tuple tuple) {
        tuples.remove(tuple);
    }

    public int tupleSize() {
        return tuples.size();
    }

    public CopyOnWriteArrayList<Tuple> getTuples() {
        return tuples;
    }
}
