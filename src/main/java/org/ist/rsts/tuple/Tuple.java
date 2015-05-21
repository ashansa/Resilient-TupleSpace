package org.ist.rsts.tuple;

import java.io.Serializable;

public class Tuple implements Serializable, Comparable<Tuple> {

    private String value1;
    private String value2;
    private String value3;

    public Tuple(String value1, String value2, String value3) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }

    public String[] getValues() {
        return new String[]{value1, value2, value3};
    }

    @Override
    public int compareTo(Tuple o) {
        String thisTupleString = value1.concat(value2).concat(value3);
        String receivedTupleString = o.value1.concat(o.value2).concat(o.value3);
        return thisTupleString.compareTo(receivedTupleString);
    }
}
