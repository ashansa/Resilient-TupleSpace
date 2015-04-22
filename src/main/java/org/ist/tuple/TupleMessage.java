package org.ist.tuple;

import org.ist.ProtocolMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TupleMessage extends ProtocolMessage implements Serializable {

    private int id;
    private Tuple tuple;
    Type type;

    public TupleMessage(int id, Tuple tuple, Type type) {
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
        type = (Type) is.readObject();
        tuple = (Tuple) is.readObject();

    }

    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Tuple getTuple() {
        return tuple;
    }
}
