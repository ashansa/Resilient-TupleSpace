package org.ist.rsts.tuple;

import org.ist.rsts.ProtocolMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TupleReply extends ProtocolMessage implements Serializable {
    Tuple[] tuples;

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {

    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {

    }
}
