package org.ist.rsts.tuple;

import org.ist.rsts.ProtocolMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TupleMessage extends ProtocolMessage implements Serializable {

    private Tuple tuple;
    Type type;

    public TupleMessage(Tuple tuple, Type type) {
        super();
        this.type = type;
        this.tuple = tuple;
    }

    public TupleMessage(byte[] buf) throws IOException{
        super(buf);
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeObject(type);
        os.writeObject(tuple);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        type = (Type) is.readObject();
        tuple = (Tuple) is.readObject();

    }

    public Type getType() {
        return type;
    }

    public Tuple getTuple() {
        return tuple;
    }
}
