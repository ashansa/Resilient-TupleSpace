package rsts;

import tfsd.ProtocolMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by ashansa on 4/16/15.
 */
public class TupleMessage extends ProtocolMessage {

    private int id;
    private String value1;
    private String value2;
    private String value3;

    /*public TupleMessage(Tuple tuple, int id) {
        super();
        this.id = id;
        this.tuple = tuple;
    }*/

    public TupleMessage(String value1, String value2, String value3, int id) {
        super();
        this.id = id;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
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
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        id=is.readInt();
        value1 = is.readUTF();
        value2 = is.readUTF();
        value3 = is.readUTF();

    }

    public String[] getValues() {
        return new String[]{value1, value2, value3};
    }

    public int getId() {
        return id;
    }
}
