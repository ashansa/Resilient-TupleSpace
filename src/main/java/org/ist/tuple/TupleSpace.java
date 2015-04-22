package org.ist.tuple;

import java.util.Vector;

/**
 * Created by ashansa on 4/15/15.
 */
public class TupleSpace {

    Vector<Tuple> tuples = new Vector<Tuple>();

    public boolean write(Tuple tuple) {
        tuples.add(tuple);
        return true;
    }

    public Vector<Tuple> read(Tuple template) {
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

    public Tuple take(Tuple template) {
        Tuple match = null;
        if (tuples.contains(template)) {
            match = template;
            tuples.remove(template);
        }
        return match;
    }

    //TODO temp method......
    public int tupleSize() {
        return tuples.size();
    }
}