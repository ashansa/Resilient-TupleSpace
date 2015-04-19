import net.sf.appia.demo.jgcs.opengroup.ProtocolMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by ashansa on 4/16/15.
 */
public class TupleMessage extends ProtocolMessage {

    Tuple tuple;

    public TupleMessage(Tuple tuple) {
        this.tuple = tuple;
    }

    @Override
    public void writeUserData(ObjectOutputStream objectOutputStream) throws IOException {

    }

    @Override
    public void readUserData(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {

    }
}
