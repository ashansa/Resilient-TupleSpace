package rsts;

import tfsd.ProtocolMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TupleMessage extends ProtocolMessage implements Serializable {

    private int id;
    private String value1;
    private String value2;
    private String value3;
    TupleManager.QueryType type;

    /*public TupleMessage(Tuple tuple, int id) {
        super();
        this.id = id;
        this.tuple = tuple;
    }*/

    public TupleMessage(String value1, String value2, String value3, TupleManager.QueryType type, int id) {
        super();
        this.id = id;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.type = type;

    }

    public TupleMessage(byte[] buf) throws IOException{
        super(buf);
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeInt(id);
        os.writeUTF(value1);
        os.writeUTF(value2);
        os.writeUTF(value3);
        os.writeObject(type);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        id=is.readInt();
        value1 = is.readUTF();
        value2 = is.readUTF();
        value3 = is.readUTF();
        type = (TupleManager.QueryType) is.readObject();

    }

    public String[] getValues() {
        return new String[]{value1, value2, value3};
    }

    public int getId() {
        return id;
    }

    public TupleManager.QueryType getType() {
        return type;
    }
}
