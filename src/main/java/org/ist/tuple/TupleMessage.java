package org.ist.tuple;

import org.ist.ProtocolMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TupleMessage extends ProtocolMessage implements Serializable {

    private int id;
    private Tuple tuple;
    TupleManager.QueryType type;

    public TupleMessage(int id, TupleManager.QueryType type, Tuple tuple) {
        super();
        this.id = id;
        this.type = type;
        this.tuple = tuple;
    }

    public TupleMessage(byte[] buf) throws IOException{
        super(buf);
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeInt(id);
        os.writeObject(type);
        os.writeObject(tuple);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        id=is.readInt();
        type = (TupleManager.QueryType) is.readObject();
        tuple = (Tuple) is.readObject();

    }

    public int getId() {
        return id;
    }

    public TupleManager.QueryType getType() {
        return type;
    }

    public Tuple getTuple() {
        return tuple;
    }
}
